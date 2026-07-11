using System.Text.Json;
using Confluent.Kafka;
using Microsoft.Extensions.Options;
using Mota.CreditProfile.Configuration;
using Mota.CreditProfile.Models;
using Mota.CreditProfile.Services;

namespace Mota.CreditProfile.Kafka;

/// <summary>
/// Long-running background service that consumes the
/// <c>income.verified</c> Kafka topic and feeds each event into the
/// CreditProfileAggregationService.
///
/// This is the C# equivalent of Spring Kafka's <c>@KafkaListener</c>
/// used by the Java services. It commits offsets after the profile has
/// been persisted, so a crash mid-processing will replay the event.
/// </summary>
public class IncomeVerifiedConsumer : BackgroundService
{
    private readonly IServiceScopeFactory _scopeFactory;
    private readonly KafkaSettings _kafkaSettings;
    private readonly ILogger<IncomeVerifiedConsumer> _logger;

    public IncomeVerifiedConsumer(
        IServiceScopeFactory scopeFactory,
        IOptions<KafkaSettings> kafkaSettings,
        ILogger<IncomeVerifiedConsumer> logger)
    {
        _scopeFactory = scopeFactory;
        _kafkaSettings = kafkaSettings.Value;
        _logger = logger;
    }

    protected override Task ExecuteAsync(CancellationToken stoppingToken)
    {
        // Run the blocking consume loop on a background thread so the
        // host's startup sequence isn't held up by the Kafka poll.
        return Task.Run(() => ConsumeLoop(stoppingToken), stoppingToken);
    }

    private async Task ConsumeLoop(CancellationToken stoppingToken)
    {
        var config = new ConsumerConfig
        {
            BootstrapServers = _kafkaSettings.BootstrapServers,
            GroupId = _kafkaSettings.GroupId,
            AutoOffsetReset = AutoOffsetReset.Earliest,
            EnableAutoCommit = false
        };

        using var consumer = new ConsumerBuilder<Ignore, string>(config).Build();
        consumer.Subscribe(_kafkaSettings.IncomeVerifiedTopic);

        _logger.LogInformation("Subscribed to Kafka topic {Topic}", _kafkaSettings.IncomeVerifiedTopic);

        var jsonOpts = new JsonSerializerOptions
        {
            PropertyNameCaseInsensitive = true
        };

        try
        {
            while (!stoppingToken.IsCancellationRequested)
            {
                try
                {
                    var result = consumer.Consume(stoppingToken);
                    if (result?.Message?.Value is null)
                    {
                        continue;
                    }

                    IncomeVerifiedEvent? evt;
                    try
                    {
                        evt = JsonSerializer.Deserialize<IncomeVerifiedEvent>(result.Message.Value, jsonOpts);
                    }
                    catch (JsonException ex)
                    {
                        _logger.LogError(ex,
                            "Failed to deserialize income.verified message; committing to skip. payload={Payload}",
                            result.Message.Value);
                        consumer.Commit(result);
                        continue;
                    }

                    if (evt is null || string.IsNullOrWhiteSpace(evt.UserId))
                    {
                        _logger.LogWarning("Skipping income.verified message with missing userId");
                        consumer.Commit(result);
                        continue;
                    }

                    using var scope = _scopeFactory.CreateScope();
                    var profileService = scope.ServiceProvider.GetRequiredService<ICreditProfileService>();
                    await profileService.UpdateFromIncomeAsync(evt, stoppingToken);

                    consumer.Commit(result);
                }
                catch (OperationCanceledException)
                {
                    break;
                }
                catch (ConsumeException ex)
                {
                    _logger.LogError(ex, "Kafka consume error: {Reason}", ex.Error.Reason);
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, "Unhandled error while processing income.verified event");
                }
            }
        }
        finally
        {
            consumer.Close();
        }
    }
}
