using System.Text.Json.Serialization;

namespace Mota.CreditProfile.Models;

public class IncomeVerifiedEvent
{
    public string UserId { get; set; } = string.Empty;
    public double AverageMonthlyIncome { get; set; }
    public double IncomeConfidenceScore { get; set; }
    public string IncomeStabilityLabel { get; set; } = string.Empty;
    public int PayEventsConsidered { get; set; }

    [JsonConverter(typeof(FlexibleDateTimeConverter))]
    public DateTime VerifiedAt { get; set; }
}