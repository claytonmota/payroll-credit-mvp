# End-to-End Demo

This walkthrough works in two environments:

- **Local development** — services running via `docker compose up --build`
  on your machine. Endpoints are on `http://localhost:8081-8084`.
- **AWS production deployment** — the same services already running on
  AWS EC2 (us-east-1). Endpoints are on `http://54.158.206.186:8081-8084`.
  No setup required — commands below work as-is against the public IP.

All commands below use `localhost`. To test against the AWS deployment,
substitute `localhost` with `54.158.206.186`.

After `docker compose up --build` (local) or with no setup (AWS), run
the following.

## 1. Send a series of payroll events for one user

To reach the `MIN_EVENTS_FOR_APPROVAL` threshold (3 events) and produce
a STABLE label, send at least 3 pay events with similar amounts:

```bash
for i in 1 2 3 4; do
  curl -X POST http://localhost:8081/v1/payroll/events \
    -H "Content-Type: application/json" \
    -d "{
      \"userId\": \"user-1001\",
      \"employerName\": \"PortMiami Logistics LLC\",
      \"payPeriodStart\": \"2026-0${i}-01\",
      \"payPeriodEnd\": \"2026-0${i}-15\",
      \"grossPay\": 3200.00,
      \"netPay\": 2450.00,
      \"payFrequency\": \"BIWEEKLY\",
      \"sourceProvider\": \"Gusto\"
    }"
  echo
done
```

Expected response for each POST:

```json
{"status":"accepted","userId":"user-1001","message":"Payroll event queued for income verification"}
```

## 2. Query income verification

```bash
curl -s http://localhost:8082/v1/income/verification/user-1001 | jq
```

Expected (approx.):

```json
{
  "userId": "user-1001",
  "averageMonthlyIncome": 5306.7,
  "incomeConfidenceScore": 1.0,
  "incomeStabilityLabel": "STABLE",
  "payEventsConsidered": 4,
  "lastUpdated": "2026-07-10T..."
}
```

## 3. Query the eligibility decision

```bash
curl -s http://localhost:8083/v1/eligibility/user-1001 | jq
```

Expected (approx.):

```json
{
  "decisionId": "…-uuid-…",
  "userId": "user-1001",
  "decision": "APPROVED",
  "creditLimitUsd": 1592.01,
  "suggestedApr": 18.99,
  "averageMonthlyIncome": 5306.7,
  "incomeConfidenceScore": 1.0,
  "incomeStabilityLabel": "STABLE",
  "reasoning": "Stable income stream with high confidence score (1.0). Average monthly income meets threshold.",
  "decidedAt": "2026-07-10T..."
}
```

## 4. Try a "credit-invisible" user with volatile income

```bash
# Wildly variable monthly income — the sort of borrower that
# historical credit scoring rejects but payroll-derived confidence
# scoring can differentiate.
for amt in 8000 500 7500 400 9000 300; do
  curl -X POST http://localhost:8081/v1/payroll/events \
    -H "Content-Type: application/json" \
    -d "{
      \"userId\": \"user-2002\",
      \"employerName\": \"Gig Aggregator\",
      \"payPeriodStart\": \"2026-01-01\",
      \"payPeriodEnd\": \"2026-01-31\",
      \"grossPay\": ${amt}.00,
      \"netPay\": ${amt}.00,
      \"payFrequency\": \"MONTHLY\",
      \"sourceProvider\": \"Rippling\"
    }"
  echo
done

curl -s http://localhost:8083/v1/eligibility/user-2002 | jq
```

Expected: `"decision": "DENIED"` or `"REVIEW"` with reasoning
explaining low confidence — showing the platform's ability to
distinguish real payment capacity from raw dollar volume.

## 5. Inspect the immutable audit trail

Every decision is retained. Query the full history:

```bash
curl -s http://localhost:8083/v1/eligibility/user-1001/history | jq
```

Or open Postgres directly:

```bash
docker compose exec postgres psql -U credit_platform -d decisions \
  -c "SELECT decision_id, decision, credit_limit_usd, income_confidence_score, decided_at FROM eligibility_decision ORDER BY decided_at DESC LIMIT 20;"
```

## 6. Query the aggregate Credit Profile (C# service)

Every `income.verified` event is also consumed by the C# credit-profile-service,
which combines the real-time income signals with a bureau lookup and stores
the result as a document in MongoDB.

```bash
curl -s http://localhost:8084/v1/credit-profile/user-1001 | jq
```

Expected shape:

```json
{
  "userId": "user-1001",
  "averageMonthlyIncome": 5306.7,
  "incomeConfidenceScore": 1.0,
  "incomeStabilityLabel": "STABLE",
  "payEventsConsidered": 4,
  "bureauScore": 712,
  "bureauSource": "Experian",
  "thinFileClassification": "STANDARD",
  "incomeHistory": [
    { "averageMonthlyIncome": 5306.7, "observedAt": "..." }
  ],
  "lastUpdated": "..."
}
```

Try a user id that ends in `-thinfile` to see the target case of the
Professional Plan — a user with real payroll-verified income but no
bureau history:

```bash
# repeat step 1 with userId=user-9999-thinfile a few times
curl -s http://localhost:8084/v1/credit-profile/user-9999-thinfile | jq
# expect "bureauScore": null, "thinFileClassification": "THIN_FILE"
```

Or inspect Mongo directly:

```bash
docker compose exec mongo mongosh creditprofile \
  --eval 'db.credit_profiles.find().limit(5).pretty()'
```
