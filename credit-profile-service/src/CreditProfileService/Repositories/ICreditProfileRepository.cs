using Mota.CreditProfile.Models;

namespace Mota.CreditProfile.Repositories;

public interface ICreditProfileRepository
{
    Task<CreditProfileDocument?> GetByUserIdAsync(string userId, CancellationToken cancellationToken = default);
    Task UpsertAsync(CreditProfileDocument profile, CancellationToken cancellationToken = default);
}