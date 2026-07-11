using Microsoft.Extensions.Logging.Abstractions;
using Moq;
using Mota.CreditProfile.Models;
using Mota.CreditProfile.Repositories;
using Mota.CreditProfile.Services;
using Xunit;

namespace Mota.CreditProfile.Tests;

public class CreditProfileAggregationServiceTests
{
    private static CreditProfileAggregationService BuildService(
        Mock<ICreditProfileRepository>? repoMock = null,
        Mock<IBureauLookupService>? bureauMock = null)
    {
        repoMock ??= new Mock<ICreditProfileRepository>();
        bureauMock ??= new Mock<IBureauLookupService>();
        return new CreditProfileAggregationService(
            repoMock.Object,
            bureauMock.Object,
            NullLogger<CreditProfileAggregationService>.Instance);
    }

    [Fact]
    public async Task UserWithNoBureauHistory_IsClassifiedAsThinFile()
    {
        var repo = new Mock<ICreditProfileRepository>();
        repo.Setup(r => r.GetByUserIdAsync(It.IsAny<string>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync((CreditProfileDocument?)null);

        var bureau = new Mock<IBureauLookupService>();
        bureau.Setup(b => b.LookupAsync(It.IsAny<string>(), It.IsAny<CancellationToken>()))
              .ReturnsAsync(new BureauLookupResult(null, "Experian", false));

        var service = BuildService(repo, bureau);

        var evt = new IncomeVerifiedEvent
        {
            UserId = "user-42",
            AverageMonthlyIncome = 4500,
            IncomeConfidenceScore = 0.9,
            IncomeStabilityLabel = "STABLE",
            PayEventsConsidered = 6,
            VerifiedAt = DateTime.UtcNow
        };

        var profile = await service.UpdateFromIncomeAsync(evt);

        Assert.Equal("THIN_FILE", profile.ThinFileClassification);
        Assert.Null(profile.BureauScore);
        repo.Verify(r => r.UpsertAsync(It.IsAny<CreditProfileDocument>(), It.IsAny<CancellationToken>()),
                    Times.Once);
    }

    [Fact]
    public async Task UserWithHighBureauScoreAndStableIncome_IsRichFile()
    {
        var repo = new Mock<ICreditProfileRepository>();
        var bureau = new Mock<IBureauLookupService>();
        bureau.Setup(b => b.LookupAsync(It.IsAny<string>(), It.IsAny<CancellationToken>()))
              .ReturnsAsync(new BureauLookupResult(760, "Equifax", true));

        var service = BuildService(repo, bureau);

        var profile = await service.UpdateFromIncomeAsync(new IncomeVerifiedEvent
        {
            UserId = "user-1",
            AverageMonthlyIncome = 8000,
            IncomeConfidenceScore = 0.95,
            IncomeStabilityLabel = "STABLE",
            PayEventsConsidered = 12,
            VerifiedAt = DateTime.UtcNow
        });

        Assert.Equal("RICH_FILE", profile.ThinFileClassification);
        Assert.Equal(760, profile.BureauScore);
    }

    [Fact]
    public async Task UserWithMidBureauScore_IsStandard()
    {
        var repo = new Mock<ICreditProfileRepository>();
        var bureau = new Mock<IBureauLookupService>();
        bureau.Setup(b => b.LookupAsync(It.IsAny<string>(), It.IsAny<CancellationToken>()))
              .ReturnsAsync(new BureauLookupResult(650, "TransUnion", true));

        var service = BuildService(repo, bureau);

        var profile = await service.UpdateFromIncomeAsync(new IncomeVerifiedEvent
        {
            UserId = "user-2",
            AverageMonthlyIncome = 5000,
            IncomeConfidenceScore = 0.7,
            IncomeStabilityLabel = "MODERATE",
            PayEventsConsidered = 5,
            VerifiedAt = DateTime.UtcNow
        });

        Assert.Equal("STANDARD", profile.ThinFileClassification);
    }

    [Fact]
    public async Task IncomeHistory_AccumulatesAcrossUpdates()
    {
        var existing = new CreditProfileDocument
        {
            UserId = "user-3",
            CreatedAt = DateTime.UtcNow.AddDays(-1),
            IncomeHistory =
            {
                new IncomeSnapshot { AverageMonthlyIncome = 3000, ObservedAt = DateTime.UtcNow.AddDays(-1) }
            }
        };

        var repo = new Mock<ICreditProfileRepository>();
        repo.Setup(r => r.GetByUserIdAsync("user-3", It.IsAny<CancellationToken>()))
            .ReturnsAsync(existing);

        var bureau = new Mock<IBureauLookupService>();
        bureau.Setup(b => b.LookupAsync(It.IsAny<string>(), It.IsAny<CancellationToken>()))
              .ReturnsAsync(new BureauLookupResult(700, "Experian", true));

        var service = BuildService(repo, bureau);

        var updated = await service.UpdateFromIncomeAsync(new IncomeVerifiedEvent
        {
            UserId = "user-3",
            AverageMonthlyIncome = 3200,
            IncomeConfidenceScore = 0.85,
            IncomeStabilityLabel = "STABLE",
            PayEventsConsidered = 4,
            VerifiedAt = DateTime.UtcNow
        });

        Assert.Equal(2, updated.IncomeHistory.Count);
        Assert.Equal(3200, updated.AverageMonthlyIncome);
    }
}

public class DeterministicBureauLookupStubTests
{
    [Fact]
    public async Task ThinfileSuffix_AlwaysReturnsNoHistory()
    {
        var stub = new DeterministicBureauLookupStub();
        var result = await stub.LookupAsync("user-abc-thinfile");
        Assert.False(result.HasHistory);
        Assert.Null(result.Score);
    }

    [Fact]
    public async Task RepeatedLookups_AreStableForSameUserId()
    {
        var stub = new DeterministicBureauLookupStub();
        var a = await stub.LookupAsync("user-9876");
        var b = await stub.LookupAsync("user-9876");
        Assert.Equal(a.Score, b.Score);
        Assert.Equal(a.Source, b.Source);
        Assert.Equal(a.HasHistory, b.HasHistory);
    }

    [Fact]
    public async Task ScoresWhenPresent_AreInFicoLikeRange()
    {
        var stub = new DeterministicBureauLookupStub();
        for (int i = 0; i < 100; i++)
        {
            var r = await stub.LookupAsync($"user-{i}");
            if (r.HasHistory)
            {
                Assert.NotNull(r.Score);
                Assert.InRange(r.Score!.Value, 580, 780);
            }
        }
    }
}
