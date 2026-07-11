using System.Globalization;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace Mota.CreditProfile.Models;

/// <summary>
/// Deserializes a DateTime from either:
///   - an ISO-8601 string ("2026-07-10T14:23:14Z"), or
///   - a Unix epoch number in seconds (with optional fractional part),
///     which is how Jackson (Java's default JSON library) serializes
///     java.time.Instant when write-dates-as-timestamps is enabled.
///
/// Interim workaround until the Schema Registry / shared contract lib
/// planned in docs/ROADMAP.md replaces ad-hoc JSON exchange.
/// </summary>
public class FlexibleDateTimeConverter : JsonConverter<DateTime>
{
    public override DateTime Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        if (reader.TokenType == JsonTokenType.Number)
        {
            var seconds = reader.GetDouble();
            return DateTimeOffset.FromUnixTimeMilliseconds((long)(seconds * 1000.0)).UtcDateTime;
        }
        if (reader.TokenType == JsonTokenType.String)
        {
            var s = reader.GetString();
            if (!string.IsNullOrEmpty(s)
                && DateTime.TryParse(s, CultureInfo.InvariantCulture,
                    DateTimeStyles.RoundtripKind | DateTimeStyles.AssumeUniversal, out var dt))
            {
                return dt.ToUniversalTime();
            }
        }
        return default;
    }

    public override void Write(Utf8JsonWriter writer, DateTime value, JsonSerializerOptions options)
    {
        writer.WriteStringValue(value.ToUniversalTime().ToString("O", CultureInfo.InvariantCulture));
    }
}