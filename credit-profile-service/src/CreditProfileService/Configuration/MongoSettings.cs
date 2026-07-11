namespace Mota.CreditProfile.Configuration;

public class MongoSettings
{
    public string ConnectionString { get; set; } = "mongodb://localhost:27017";
    public string DatabaseName { get; set; } = "creditprofile";
    public string CreditProfileCollection { get; set; } = "credit_profiles";
}
