namespace Mota.CreditProfile.Configuration;

public class KafkaSettings
{
    public string BootstrapServers { get; set; } = "localhost:9092";
    public string GroupId { get; set; } = "credit-profile-service";
    public string IncomeVerifiedTopic { get; set; } = "income.verified";
}
