# RFE Evidence — Reading Guide

This document is a guide for USCIS adjudicators, the petitioner's
counsel, or any authorized reviewer of the I-140 / EB-2 NIW petition
of **Clayton Soares da Mota** (petition filed April 27, 2026; RFE
issued June 22, 2026; response due September 17, 2026).

Its purpose is to map each substantive technical claim made in the
originally submitted **Professional Plan** (February 2026) to concrete,
running, version-controlled evidence in this repository. The intent is
to demonstrate — under the second prong of *Matter of Dhanasar* — that
the petitioner is well positioned to advance the proposed endeavor:
not through assertion, but through implementation.

## 1. Live deployment

The MVP described below is deployed on Amazon Web Services (AWS EC2,
region `us-east-1`) under a dedicated domain, `payroll-credit.com`, and
is reachable from any browser or command-line HTTP client over HTTPS at
the following public endpoints:

| Service | Purpose | Public endpoint |
|---|---|---|
| ingestion-service | Receives payroll-provider events | `POST https://ingestion.payroll-credit.com/v1/payroll/events` |
| income-verification-service | Real-time income confidence scoring | `GET  https://income.payroll-credit.com/v1/income/verification/{userId}` |
| decision-service | Rules-based eligibility engine | `GET  https://decision.payroll-credit.com/v1/eligibility/{userId}` |
| credit-profile-service | Aggregate credit profile (Java + C# interop) | `GET  https://creditprofile.payroll-credit.com/v1/credit-profile/{userId}` |

Complete step-by-step reproduction of the end-to-end flow is documented
in [`docs/DEMO.md`](DEMO.md). Anyone can send a payroll event and observe
it flow through Kafka, be persisted in PostgreSQL and MongoDB, and
produce an auditable credit decision — in real time.

## 2. Mapping — Professional Plan to Implementation

The Professional Plan proposed three methodologies for modernizing the
U.S. credit assessment infrastructure. Each is implemented in this
repository as follows.

### 2.1 Methodology 1 — Integrated Data Architecture & Interoperability

> *"Deploy a distributed data architecture that will eliminate the
> extreme fragmentation of the U.S. credit market by synchronizing
> disparate information sources into a single, cohesive source of
> truth."* (Professional Plan §3, Methodology 1)

**Implementation:**

| Plan component | Where in repo |
|---|---|
| Payroll provider integration | [`ingestion-service/`](../ingestion-service/) — normalized event contract for ADP, Workday, Paychex, Gusto, Rippling |
| Event streaming backbone | Apache Kafka topics `payroll.events` and `income.verified` (see [`docker-compose.yml`](../docker-compose.yml)) |
| Single source of truth for income | [`income-verification-service/`](../income-verification-service/) — persists per-user aggregate to PostgreSQL |
| Multi-store data layer | PostgreSQL (transactional) + MongoDB (aggregate profiles, see [`credit-profile-service/`](../credit-profile-service/)) |

**Verifiable:** send an event to `POST /v1/payroll/events` and observe
its normalized form persisted across two databases and two languages.

### 2.2 Methodology 2 — Cloud-Native Real-Time Risk Orchestration

> *"Advanced orchestration of microservices architectures developed in
> Java (Spring Boot) and C# (.NET), deployed within cloud-native
> environments."* (Professional Plan §3, Methodology 2)

**Implementation:**

| Plan component | Where in repo |
|---|---|
| Java Spring Boot services | [`ingestion-service/`](../ingestion-service/), [`income-verification-service/`](../income-verification-service/), [`decision-service/`](../decision-service/) — all Spring Boot 3, Java 17 |
| C# .NET services | [`credit-profile-service/`](../credit-profile-service/) — .NET 8 minimal-host ASP.NET Core |
| Event-driven, not request-driven | Communication happens via Kafka events — services publish and consume asynchronously |
| Cloud-native deployment | AWS EC2 running Docker Compose. Terraform skeleton in [`infra/terraform/`](../infra/terraform/) |

**Verifiable:** the same JSON event flows from a Java producer into a C#
consumer without a shared library, proving the language-agnostic
contract discipline. See [ADR-001](#adr-001-producers-do-not-emit-kafka-type-headers).

### 2.3 Methodology 3 — Advanced Data Governance & Financial Compliance

> *"Automated validation mechanisms, full audit trails, and data
> modeling frameworks aligned with international standards such as
> Basel II, ensuring that all credit decisions are backed by verifiable
> and secure data flows."* (Professional Plan §3, Methodology 3)

**Implementation:**

| Plan component | Where in repo |
|---|---|
| Immutable audit records | [`decision-service/.../EligibilityDecision.java`](../decision-service/src/main/java/com/mota/decision/model/EligibilityDecision.java) — append-only `eligibility_decision` table |
| Human-readable reasoning | Every decision persists a `reasoning` field with plain-language justification — supports adverse action reporting (CFPB, ECOA reason codes) |
| Rules engine (not opaque ML) | [`decision-service/.../EligibilityRulesEngine.java`](../decision-service/src/main/java/com/mota/decision/service/EligibilityRulesEngine.java) — deterministic, testable, explainable |
| Basel II data-quality alignment | Deterministic, auditable data lineage from raw event → decision |

**Verifiable:** `SELECT decision_id, decision, reasoning FROM eligibility_decision`
on the deployed PostgreSQL — every decision on record, with the reason
each was made.

## 3. Core technical claim of the endeavor

The Professional Plan's central promise is to replace static, historical
credit scores with real-time, payroll-derived income validation — the
mechanism that enables financial inclusion for thin-file consumers.

**Implementation:** [`IncomeValidationService.java`](../income-verification-service/src/main/java/com/mota/incomeverification/service/IncomeValidationService.java)

The service computes an **income confidence score** from the coefficient
of variation of a user's payroll history (up to 12 pay events). Users
are classified as `STABLE`, `MODERATE`, `VOLATILE`, or `INSUFFICIENT_DATA`.
The classification does **not** depend on a traditional bureau score.

**This is the demonstrable technical difference from routine database
architecture:** a person with no bureau history but stable payroll
income receives a legitimate positive assessment, while a person with a
high raw income but volatile earnings receives a warning. This is the
mechanism through which the endeavor achieves the financial-inclusion
outcome claimed in the Professional Plan.

## 4. Financial inclusion — the thin-file case

The endeavor's central social contribution is to serve the 26 million
Americans classified as "credit invisible" by the CFPB, plus 19 million
with unscored files (see Professional Plan §6).

**Implementation:** [`CreditProfileAggregationService.cs`](../credit-profile-service/src/CreditProfileService/Services/CreditProfileAggregationService.cs)

The `ThinFileClassification` field is set to `THIN_FILE` when a user
has stable, payroll-verified income but no bureau history. Downstream
consumers can differentiate this case from `STANDARD` (has bureau
history) and `RICH_FILE` (high score plus stable income).

**Live example:**

```bash
# Send payroll events for a user whose ID ends in -thinfile
# (deterministic stub simulates no bureau history)
curl https://creditprofile.payroll-credit.com/v1/credit-profile/user-9999-thinfile
# Returns: "bureauScore": null, "thinFileClassification": "THIN_FILE"
```

## 5. Software engineering rigor

The following are objective indicators, verifiable directly in the repo:

- **Version control:** Git history at github.com/claytonmota/payroll-credit-mvp — all commits authored by Clayton Soares da Mota
- **Unit tests:**
  - [`income-verification-service/src/test/`](../income-verification-service/src/test/) — 4 tests covering confidence scoring, frequency normalization, edge cases
  - [`decision-service/src/test/`](../decision-service/src/test/) — 5 tests covering all rule branches
  - [`credit-profile-service/tests/`](../credit-profile-service/tests/CreditProfileService.Tests/) — 7 xUnit tests including THIN_FILE classification
- **Reproducible builds:** `docker compose build` completes cleanly for all 4 services
- **Reproducible demo:** [`docs/DEMO.md`](DEMO.md) provides a full end-to-end script
- **Open source license:** Apache 2.0 (see [`LICENSE`](../LICENSE))
- **Contribution guidelines:** [`CONTRIBUTING.md`](../CONTRIBUTING.md)
- **Architecture Decision Records:** documented in this file (see §7)

## 6. Prior work referenced in the Professional Plan

The Professional Plan cites three prior case studies from the
petitioner's 25-year career in Brazilian banking (Bradesco, Itaú
Unibanco). Those projects are proprietary IP of the respective
institutions and cannot be reproduced in this open repository.

What **can** be demonstrated is that the architectural patterns claimed
to have produced those results — cloud-native microservices, event-driven
orchestration, deterministic rules engines, audit-trail persistence —
are the exact same patterns implemented here. The scaling result cited in
the Professional Plan (Bradesco: 4,000 → 42,000 daily eligibility
checks, a 10x improvement using Microsoft Azure and FICO decision engines)
is achievable in this architecture because the design is horizontally
scalable by Kafka consumer groups and stateless microservices.

## 7. Architecture Decision Records

### ADR-001: Producers do not emit Kafka type headers

**Context:** Java Spring Kafka's `JsonSerializer` emits a `__TypeId__`
header on every message, referencing the producer's internal Java class
name. Consumers with `JsonDeserializer` then require that class to be
in a trust list.

**Decision:** All Kafka producers in this project set
`ADD_TYPE_INFO_HEADERS = false`. Each consumer owns its own DTO in its
own package (`com.mota.<service>.model.*`). The event **schema** is the
contract, not the Java class name.

**Consequence:** Services remain independently deployable across
languages (Java Spring Boot ↔ C# .NET) and across teams. A future
service written in Python, Go, or Rust can consume the same topics with
no interop concerns. Trade-off: consumers must know the target type at
compile time; there is no auto-mapping via headers.

**Reference:** [`ingestion-service/.../KafkaProducerConfig.java`](../ingestion-service/src/main/java/com/mota/ingestion/config/KafkaProducerConfig.java),
[`income-verification-service/.../KafkaConfig.java`](../income-verification-service/src/main/java/com/mota/incomeverification/config/KafkaConfig.java)

## 8. Timeline of construction

The following are milestone dates verifiable via `git log` on the
public repository:

- **First commit** — initial MVP scaffold (July 2026)
- **credit-profile-service (C#)** — cross-language demonstration
- **AWS production deploy** — live public endpoints
- **End-to-end pipeline verified in production** — first successful
  `APPROVED` decision returned from a `curl` originating outside the
  AWS VPC
- **This document** — reading guide committed to repo

Reviewers can verify the timeline of authorship independently via:

```bash
git log --format='%h %ad %an %s' --date=short
```

## 9. Not yet implemented (transparency)

To ensure this document is accurate and does not overstate the state of
the endeavor, the following architectural blocks are **planned** in
[`docs/ROADMAP.md`](ROADMAP.md) but **not yet implemented** in this
iteration of the MVP:

- Identity Service (OAuth 2.0 / OIDC, consent management)
- Employment Service (HR-feed integration)
- Real HTTP adapter to credit bureaus (deterministic stub is in place)
- External API Gateway (Kong / Apigee) with JWT verification
- Notification Service (email / SMS / push)
- Data Warehouse export to Snowflake / Redshift
- Full observability stack (Prometheus / Jaeger / ELK)

These are omissions of scope, not of capability. The engineering
approach demonstrated in the four services already in place applies
identically to the remaining blocks.

## 10. Reviewer's quick-start (5 minutes)

For a reviewer who wants to independently verify this evidence in five
minutes, without cloning the repository:

```bash
# 1. Send a payroll event
curl -X POST https://ingestion.payroll-credit.com/v1/payroll/events \
  -H "Content-Type: application/json" \
  -d '{"userId":"reviewer-001","employerName":"Reviewer Test","payPeriodStart":"2026-06-01","payPeriodEnd":"2026-06-15","grossPay":3200.00,"netPay":2450.00,"payFrequency":"BIWEEKLY","sourceProvider":"Gusto"}'

# 2. Send three more (to reach the STABLE threshold)
# Repeat step 1 three more times with different payPeriodStart/End

# 3. Wait ~10 seconds and query the decision
curl https://decision.payroll-credit.com/v1/eligibility/reviewer-001

# 4. Query the aggregate credit profile
curl https://creditprofile.payroll-credit.com/v1/credit-profile/reviewer-001
```

The complete response from step 3 should be a JSON object containing a
`decision` field (`APPROVED`, `REVIEW`, or `DENIED`), a `creditLimitUsd`,
a `suggestedApr`, and a `reasoning` field with the human-readable
justification for the decision. This is the endeavor's end-to-end
technical claim, made observable in a single command.

---

*Prepared by Clayton Soares da Mota as part of the response to the
I-140 / EB-2 NIW Request for Evidence issued by USCIS Texas Service
Center, June 22, 2026. This document is a reading guide only. The
formal response to the RFE will be submitted through counsel of record.*
