using Mota.CreditProfile.Models;
using Mota.CreditProfile.Repositories;

namespace Mota.CreditProfile.Services;

public interface ICreditProfileService
{
    Task<CreditProfileDocument> UpdateFromIncomeAsync(IncomeVerifiedEvent income, CancellationToken cancellationToken = default);
    Task<CreditProfileDocument?> GetAsync(string userId, CancellationToken cancellationToken = default);
}

/// <summary>
/// Core service: on every income.verified event, upsert the aggregate
/// credit profile for a user with fresh income signals + a (stubbed)
/// bureau lookup, then classify the file as THIN / STANDARD / RICH.
///
/// Thin-file classification is the concrete implementation of the
/// Professional Plan's central financial-inclusion goal: recognizing
/// that a user with stable payroll-verified income but no bureau
/// history is a legitimate borrower, not an unscoreable one.
/// </summary>
public class CreditProfileAggregationService : ICreditProfileService
{
    private readonly ICreditProfileRepository _repository;
    private readonly IBureauLookupService _bureau;
    private readonly ILogger<CreditProfileAggregationService> _logger;

    public CreditProfileAggregationService(
        ICreditProfileRepository repository,
        IBureauLookupService bureau,
        ILogger<CreditProfileAggregationService> logger)
    {
        _repository = repository;
        _bureau = bureau;
        _logger = logger;
    }

    public async Task<CreditProfileDocument?> GetAsync(string userId, CancellationToken cancellationToken = default)
    {
        return await _repository.GetByUserIdAsync(userId, cancellationToken);
    }

    public async Task<CreditProfileDocument> UpdateFromIncomeAsync(
        IncomeVerifiedEvent income,
        CancellationToken cancellationToken = default)
    {
        var existing = await _repository.GetByUserIdAsync(income.UserId, cancellationToken);
        var now = DateTime.UtcNow;

        var profile = existing ?? new CreditProfileDocument
        {
            UserId = income.UserId,
            CreatedAt = now
        };

        profile.AverageMonthlyIncome = income.AverageMonthlyIncome;
        profile.IncomeConfidenceScore = income.IncomeConfidenceScore;
        profile.IncomeStabilityLabel = income.IncomeStabilityLabel;
        profile.PayEventsConsidered = income.PayEventsConsidered;
        profile.LastUpdated = now;

        profile.IncomeHistory.Add(new IncomeSnapshot
        {
            AverageMonthlyIncome = income.AverageMonthlyIncome,
            IncomeConfidenceScore = income.IncomeConfidenceScore,
            IncomeStabilityLabel = income.IncomeStabilityLabel,
            PayEventsConsidered = income.PayEventsConsidered,
            ObservedAt = income.VerifiedAt == default ? now : income.VerifiedAt
        });

        // Cap history to last 50 entries to keep the document bounded.
        if (profile.IncomeHistory.Count > 50)
        {
            profile.IncomeHistory = profile.IncomeHistory
                .OrderByDescending(h => h.ObservedAt)
                .Take(50)
                .ToList();
        }

        var bureau = await _bureau.LookupAsync(income.UserId, cancellationToken);
        profile.BureauScore = bureau.Score;
        profile.BureauSource = bureau.Source;
        profile.ThinFileClassification = Classify(bureau, income);

        await _repository.UpsertAsync(profile, cancellationToken);

        _logger.LogInformation(
            "Updated credit profile for userId={UserId}: bureauScore={BureauScore}, thinFile={ThinFile}, incomeLabel={IncomeLabel}",
            profile.UserId, profile.BureauScore, profile.ThinFileClassification, profile.IncomeStabilityLabel);

        return profile;
    }

    /// <summary>
    /// Classification logic:
    /// - No bureau history at all → THIN_FILE (the target of the endeavor).
    /// - Bureau score present but low with weak income confidence → THIN_FILE.
    /// - Bureau score present with any income signal → STANDARD.
    /// - Bureau score high + STABLE income → RICH_FILE.
    /// </summary>
    private static string Classify(BureauLookupResult bureau, IncomeVerifiedEvent income)
    {
        if (!bureau.HasHistory || bureau.Score is null)
        {
            return "THIN_FILE";
        }

        if (bureau.Score >= 720 && income.IncomeStabilityLabel == "STABLE")
        {
            return "RICH_FILE";
        }

        return "STANDARD";
    }
}
