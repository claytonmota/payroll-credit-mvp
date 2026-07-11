# Roadmap

Mapping between the architecture diagram (v4) and this repository's
implementation state, so anyone reviewing the code can see exactly what
is delivered vs planned.

## Delivered in this iteration (v0.1.0)

| Diagram component               | Where it lives in the code |
|----------------------------------|-----------------------------|
| Ingestion Layer → API Gateway   | `ingestion-service` REST endpoint `/v1/payroll/events` (a real Kong/Apigee gateway is out of scope for the MVP) |
| Ingestion Layer → Webhook Receiver | Same endpoint, accepts a normalized payroll event contract |
| Event Streaming → Kafka topics   | `payroll.events` (produced by ingestion) and `income.verified` (produced by income-verification) |
| Microservices → Payroll Service (partial) | `PayrollEvent` normalization + persistence to `payroll_event_record` |
| Microservices → Income Verification Service | Full: `IncomeValidationService` computes real-time confidence score |
| Microservices → Risk Orchestration Engine (rules subset) | `EligibilityRulesEngine` — deterministic rules, no ML |
| Microservices → Decision Service | `EligibilityController` + persisted `eligibility_decision` |
| Microservices → Credit Profile Service | `credit-profile-service` (C# .NET 8) — consumes `income.verified`, aggregates income + bureau lookup into MongoDB documents, classifies THIN_FILE / STANDARD / RICH_FILE. Bureau adapter is a deterministic stub for now (see below). |
| Microservices → Audit Service (partial) | Immutable `eligibility_decision` audit table with human-readable `reasoning` |
| Data Layer → Operational DB (PostgreSQL) | Postgres schemas `incomeverification`, `decisions` |
| Data Layer → Document / NoSQL Store (MongoDB) | Mongo database `creditprofile`, collection `credit_profiles` (used by `credit-profile-service`) |
| Cross-cutting → Observability (partial) | Structured `slf4j` logs; Actuator `/actuator/health` |

## Not yet built (planned)

| Diagram component | Notes |
|---|---|
| Identity Service (OAuth/OIDC, Consent Mgmt) | Placeholder for future iteration |
| Employment Service | Would consume employer HR feeds and score job tenure |
| Account Aggregation Service | Plaid/MX integration |
| Credit Profile Service → real bureau HTTP adapter | Deterministic stub is in place today; a real HTTP adapter to Experian/Equifax/TransUnion sandboxes is the next step for this service |
| Affordability Service | DTI + expense analysis using bank transaction data |
| Notification Service | Email/SMS/push notifications |
| API Gateway (external) | Kong/Apigee, rate limiting, throttling |
| SQL Server operational DB | Currently PostgreSQL only; SQL Server is called out in the Professional Plan and can be added as a separate schema for one of the future services |
| DynamoDB / MongoDB stores | Same — planned for services with document-shaped payloads |
| Data Warehouse (Snowflake / Redshift) | Not needed for MVP |
| Cross-cutting: full observability (ELK / Prometheus / Jaeger) | Actuator today; Prometheus scraping trivial to add |
| Security & Compliance layer | JWT auth on API Gateway, secrets manager, TLS everywhere in cloud deployment |
| Cloud deployment (Terraform) | Skeleton in `infra/terraform/` |

## Suggested next iterations (in priority order)

1. **Real bureau HTTP adapter for the credit-profile-service** — replace
   the deterministic stub with an actual sandbox integration against
   one of Experian / Equifax / TransUnion's developer APIs. This closes
   the last remaining gap between the stub and the block shown in the
   architecture diagram.
2. **Basic API Gateway** — an nginx or Spring Cloud Gateway in front,
   with JWT verification, closing the "External API Gateway" box.
3. **Cloud deployment** — flesh out the Terraform module and deploy to
   AWS (EKS + MSK + RDS + DocumentDB) or Azure (AKS + Event Hubs +
   PostgreSQL + Cosmos DB). Even one working environment is powerful
   evidence.
4. **Data Warehouse export** — a periodic dbt job that ships
   `eligibility_decision` rows to Snowflake for analytics.
5. **Public deployment + LOI** — deploy to a public URL and reach out
   to one credit union or fintech for a formal letter of interest.
   This is the strongest single evidence artifact for the second NIW
   prong.
