using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace Mota.CreditProfile.Models;

/// <summary>
/// Aggregate credit profile per user, persisted as a MongoDB document.
///
/// This is the "Credit Profile Service" block from the architecture
/// diagram. It joins three data views into a single, queryable document:
///   1. Real-time income signals (from income-verification-service).
///   2. Simulated credit bureau signals (Experian / Equifax / TransUnion
///      — stubbed in this MVP; a real Bureau Integration adapter is
///      out of scope for this iteration).
///   3. Thin-file classification derived from both.
///
/// Modeled as a document (rather than a set of relational tables)
/// because credit profiles are read/written as a whole for a given
/// user, and their shape evolves over time as new bureau sources and
/// signals are added — exactly the case MongoDB is chosen for in the
/// Professional Plan's Methodology 1.
/// </summary>
[BsonIgnoreExtraElements]
public class CreditProfileDocument
{
    /// <summary>User identifier — also the Mongo document _id.</summary>
    [BsonId]
    [BsonRepresentation(BsonType.String)]
    public string UserId { get; set; } = string.Empty;

    /// <summary>Latest observed average monthly income.</summary>
    public double? AverageMonthlyIncome { get; set; }

    /// <summary>Latest income confidence score (0.0 – 1.0).</summary>
    public double? IncomeConfidenceScore { get; set; }

    /// <summary>STABLE / MODERATE / VOLATILE / INSUFFICIENT_DATA.</summary>
    public string? IncomeStabilityLabel { get; set; }

    /// <summary>How many pay events were considered in the score.</summary>
    public int? PayEventsConsidered { get; set; }

    /// <summary>Simulated FICO-like traditional bureau score. Nullable
    /// for users with no bureau history ("thin file").</summary>
    public int? BureauScore { get; set; }

    /// <summary>Which of the three bureaus supplied the above score.</summary>
    public string? BureauSource { get; set; }

    /// <summary>THIN_FILE / STANDARD / RICH_FILE — derived from bureau
    /// data availability.</summary>
    public string ThinFileClassification { get; set; } = "UNKNOWN";

    /// <summary>Chronological log of every income.verified update
    /// received for this user, for auditability.</summary>
    public List<IncomeSnapshot> IncomeHistory { get; set; } = new();

    public DateTime LastUpdated { get; set; }
    public DateTime CreatedAt { get; set; }
}

public class IncomeSnapshot
{
    public double AverageMonthlyIncome { get; set; }
    public double IncomeConfidenceScore { get; set; }
    public string IncomeStabilityLabel { get; set; } = string.Empty;
    public int PayEventsConsidered { get; set; }
    public DateTime ObservedAt { get; set; }
}
