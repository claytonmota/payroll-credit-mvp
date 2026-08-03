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

Formal OpenAPI 3.0.3 specifications for all four services are available in
[`docs/api/`](api/). They can be imported directly into Postman, Insomnia, or
any OpenAPI viewer to explore and exercise the platform without cloning the
repository.

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
contract discipline. See [ADR-0001](adr/0001-kafka-producers-omit-type-headers.md).

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
- **Formal API contracts:** OpenAPI 3.0.3 specifications for all four services
  in [`docs/api/`](api/) — [ingestion](api/ingestion-service.openapi.yaml),
  [income verification](api/income-verification-service.openapi.yaml),
  [decision](api/decision-service.openapi.yaml),
  [credit profile](api/credit-profile-service.openapi.yaml). Beyond request and
  response shapes, these record the pay-frequency normalization multipliers,
  the confidence-score threshold table, the ordered rules the eligibility
  engine evaluates, the credit limit and APR sizing policy, and the thin-file
  classification conditions. They are executable contracts rather than
  documentation: a consuming institution could generate a client library and
  integrate without reading the source.
- **Recorded demonstration:** a continuous, unedited screen recording of the
  full pipeline exercised against the live deployment — health checks across
  all four public subdomains from a workstation over the public internet, four
  payroll events submitted, the income confidence score computed, an
  eligibility decision returned with its reasoning, the thin-file case showing
  a null bureau score against verified income, and the resulting records read
  back from PostgreSQL and MongoDB on the host. Single take, no cuts, no
  post-production: every response shown is a live HTTP response at the time of
  recording. Held privately and provided to counsel of record — see
  [`docs/evidence/README.md`](evidence/README.md).
- **Data model reference:** [`docs/data-model/`](data-model/) — entity
  relationship diagrams, explicit DDL for both PostgreSQL databases, a MongoDB
  schema validator, a field-level data dictionary, a lineage trace following one
  value through the pipeline, and classification of each field against GLBA,
  FCRA and Regulation B. The DDL was verified against the running deployment on
  2026-07-26 and corrected where it diverged; the verification is recorded in
  the document, and `schema/verify-schema.sh` reproduces it.
- **Processing flow diagrams:** [`docs/diagrams/`](diagrams/) — five flows
  covering end-to-end processing, thin-file classification, cross-language event
  fan-out, assessment evolution with the append-only audit trail, and deployment
  topology. Written in Mermaid rather than exported from a drawing tool, so they
  are plain text, reviewable in a diff, and cannot fall silently out of step
  with the implementation.
- **Architecture Decision Records:** four records in [`docs/adr/`](adr/) —
  Kafka type headers, deterministic rules over machine learning, denormalised
  audit inputs, and polyglot persistence. Each states the trade-off accepted,
  not only the benefit obtained (see §7)
- **System architecture documentation:** [`docs/System_Architecture_Document.pdf`](System_Architecture_Document.pdf)
  — a formal 27-page engineering document covering system context,
  architectural principles, per-service specification, data model, API
  reference, technology selection rationale, non-functional characteristics,
  and known gaps. It summarises the architecture decision records rather than
  restating them, so that it cannot drift out of step with `docs/adr/`.

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

Significant design choices are recorded as Architecture Decision Records in
[`docs/adr/`](adr/), one Markdown file per decision, following the structure
proposed by Michael Nygard: status, context, decision, consequences.

They exist because the reasoning behind a decision decays faster than the code
implementing it. The code shows what was done; only a record shows why, and
what was given up.

| Record | Decision | Principal trade-off accepted |
|---|---|---|
| [ADR-0001](adr/0001-kafka-producers-omit-type-headers.md) | Kafka producers omit type headers | Consumers must know their target type at compile time; no automatic type resolution from the message |
| [ADR-0002](adr/0002-deterministic-rules-engine-over-machine-learning.md) | Deterministic rules engine over machine learning | Predictive accuracy is almost certainly lower than a trained model would achieve, and the size of that gap is unknown |
| [ADR-0003](adr/0003-input-signals-denormalised-into-audit-record.md) | Input signals denormalised into the audit record | The same value is stored in several places, and deletion under CCPA becomes difficult against an append-only table |
| [ADR-0004](adr/0004-polyglot-persistence.md) | Polyglot persistence — PostgreSQL and MongoDB | No referential integrity across the stores, larger operational surface, and a contributor must know both |

Two of these bear directly on the regulatory claims made in §2.3 and §3, and
are the ones worth reading in full.

**[ADR-0002](adr/0002-deterministic-rules-engine-over-machine-learning.md)**
records a decision taken *against* the prevailing empirical evidence.
FinRegLab's 2025 study *Advancing the Credit Ecosystem: Machine Learning &
Cash Flow Data in Consumer Underwriting* found that combining machine learning
with cash-flow data produced the largest gains in predictiveness — and
expanded credit access without increasing lender default risk. This platform
nonetheless uses a deterministic rules engine, because Regulation B requires a
specific principal reason on adverse action, Basel II requires reproducibility,
and there is no historical loan performance data available to train a model on.

The record states plainly that the accuracy cost of that choice is unknown, and
that if a trained model would approve creditworthy applicants these rules
decline, the cost falls on precisely the population the endeavor exists to
serve. It is defensible for a prototype without performance data; it would
require re-examination against measured outcomes before production. That
transparency is the point: a record listing only benefits would be marketing
rather than engineering.

**[ADR-0003](adr/0003-input-signals-denormalised-into-audit-record.md)**
explains why the eligibility audit record stores copies of its own input
signals rather than referencing them. Those values also live in a table holding
one mutable row per worker, overwritten on every new pay event. An audit record
referencing that table would, months later, reconstruct a past decision using
today's income figure — and produce a different answer. Freezing the inputs at
decision time is what makes a decision independently reproducible, which is
what Basel II model risk management asks for and what Regulation B presumes
when it obliges a lender to state the reason that actually applied.

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

Reviewers who prefer a graphical client can import the four specifications
from [`docs/api/`](api/) into Postman or Insomnia instead, which generates a
complete request collection with example bodies already filled in.

---

*Prepared by Clayton Soares da Mota as part of the response to the
I-140 / EB-2 NIW Request for Evidence issued by USCIS Texas Service
Center, June 22, 2026. This document is a reading guide only. The
formal response to the RFE will be submitted through counsel of record.*
