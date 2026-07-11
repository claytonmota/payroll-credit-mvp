using Microsoft.AspNetCore.Mvc;
using Mota.CreditProfile.Services;

namespace Mota.CreditProfile.Controllers;

[ApiController]
[Route("v1/credit-profile")]
public class CreditProfileController : ControllerBase
{
    private readonly ICreditProfileService _service;

    public CreditProfileController(ICreditProfileService service)
    {
        _service = service;
    }

    /// <summary>
    /// Returns the current aggregate credit profile for a user, or 404
    /// if none has been built yet (no income.verified events observed).
    /// </summary>
    [HttpGet("{userId}")]
    public async Task<IActionResult> Get(string userId, CancellationToken cancellationToken)
    {
        var profile = await _service.GetAsync(userId, cancellationToken);
        if (profile is null)
        {
            return NotFound(new
            {
                error = "not_found",
                message = $"No credit profile yet for userId={userId}"
            });
        }
        return Ok(profile);
    }

    [HttpGet("health")]
    public IActionResult Health()
    {
        return Ok(new { service = "credit-profile-service", status = "UP" });
    }
}
