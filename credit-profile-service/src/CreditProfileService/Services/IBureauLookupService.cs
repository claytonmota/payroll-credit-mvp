namespace Mota.CreditProfile.Services;

/// <summary>
/// Abstraction for a traditional credit bureau lookup (Experian /
/// Equifax / TransUnion / FICO). In this MVP a deterministic stub is
/// used; a real HTTP adapter to a bureau API is planned in
/// docs/ROADMAP.md.
/// </summary>
public interface IBureauLookupService
{
    Task<BureauLookupResult> LookupAsync(string userId, CancellationToken cancellationToken = default);
}

public record BureauLookupResult(int? Score, string Source, bool HasHistory);
