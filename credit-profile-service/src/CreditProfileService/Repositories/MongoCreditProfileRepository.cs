using Microsoft.Extensions.Options;
using MongoDB.Driver;
using Mota.CreditProfile.Configuration;
using Mota.CreditProfile.Models;

namespace Mota.CreditProfile.Repositories;

public class MongoCreditProfileRepository : ICreditProfileRepository
{
    private readonly IMongoCollection<CreditProfileDocument> _collection;

    public MongoCreditProfileRepository(IOptions<MongoSettings> settings)
    {
        var config = settings.Value;
        var client = new MongoClient(config.ConnectionString);
        var db = client.GetDatabase(config.DatabaseName);
        _collection = db.GetCollection<CreditProfileDocument>(config.CreditProfileCollection);
    }

    public async Task<CreditProfileDocument?> GetByUserIdAsync(string userId, CancellationToken cancellationToken = default)
    {
        return await _collection
            .Find(p => p.UserId == userId)
            .FirstOrDefaultAsync(cancellationToken);
    }

    public async Task UpsertAsync(CreditProfileDocument profile, CancellationToken cancellationToken = default)
    {
        var filter = Builders<CreditProfileDocument>.Filter.Eq(p => p.UserId, profile.UserId);
        await _collection.ReplaceOneAsync(
            filter,
            profile,
            new ReplaceOptions { IsUpsert = true },
            cancellationToken);
    }
}