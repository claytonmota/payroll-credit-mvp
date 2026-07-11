namespace Mota.CreditProfile.Services;

/// <summary>
/// Deterministic stub of a credit bureau lookup used until a real
/// bureau adapter is implemented.
///
/// Behavior is intentionally reproducible so that end-to-end tests are
/// stable:
/// - userIds ending in "-thinfile" or with hash 0-2 mod 10 return no
///   bureau history (thin-file scenario — the core case the Professional
///   Plan aims to serve).
/// - Other userIds return a pseudo-random but stable score in [580, 780]
///   drawn from a deterministic hash of the userId.
/// - Source alternates deterministically between Experian, Equifax and
///   TransUnion.
/// </summary>
public class DeterministicBureauLookupStub : IBureauLookupService
{
    private static readonly string[] Sources = { "Experian", "Equifax", "TransUnion" };

    public Task<BureauLookupResult> LookupAsync(string userId, CancellationToken cancellationToken = default)
    {
        var hash = StableHash(userId);
        var noHistory = userId.EndsWith("-thinfile", StringComparison.OrdinalIgnoreCase)
                        || hash % 10 < 3;

        if (noHistory)
        {
            return Task.FromResult(new BureauLookupResult(null, Sources[hash % 3], false));
        }

        // Map hash into a plausible FICO-like range [580, 780].
        var score = 580 + (int)(hash % 201);
        return Task.FromResult(new BureauLookupResult(score, Sources[hash % 3], true));
    }

    private static uint StableHash(string s)
    {
        // FNV-1a — deterministic across runs & platforms.
        const uint offset = 2166136261u;
        const uint prime = 16777619u;
        var h = offset;
        foreach (var c in s)
        {
            h ^= c;
            h *= prime;
        }
        return h;
    }
}
