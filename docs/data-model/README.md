# Data Model

Physical data model for the Payroll-Integrated Real-Time Credit
Infrastructure: entity relationships, explicit DDL, field-level semantics,
data lineage through the pipeline, and privacy classification.

Section 6 of the System Architecture Document summarises the same structures
in tabular form. This document is the detailed reference — it adds the
relationship diagrams, the executable DDL, the lineage trace, and the
regulatory classification of each field.

| Section | Contents |
|---|---|
| [1. Store topology](#1--store-topology) | Which service owns which database, and why |
| [2. Entity relationships](#2--entity-relationships) | ER diagrams per database |
| [3. Explicit DDL](#3--explicit-ddl) | The relational schema as SQL |
| [4. Document schema](#4--document-schema) | MongoDB structure and validator |
| [5. Data dictionary](#5--data-dictionary) | Field-level meaning, not just type |
| [6. Data lineage](#6--data-lineage) | How one value travels the pipeline |
| [7. Privacy classification](#7--privacy-classification) | GLBA, CCPA, FCRA exposure |
| [8. Known gaps](#8--known-gaps) | Indexes, constraints, retention |

---

## 1 — Store topology

Three logical databases across two engines. Each is owned by exactly one
service; no service reads another service's tables.

| Database | Engine | Owning service | Contents |
|---|---|---|---|
| `incomeverification` | PostgreSQL 16 | income-verification-service | Raw pay events and the current income assessment |
| `decisions` | PostgreSQL 16 | decision-service | Append-only eligibility decision audit trail |
| `creditprofile` | MongoDB 7 | credit-profile-service | Aggregate profile documents |

**Why the split.** Service ownership of schema is what makes the services
independently deployable. If the decision service read
`income_verification_result` directly, a column rename in the income service
would break it, and the two would have to be released together. Instead the
contract between them is the `income.verified` Kafka event, which is versioned
and explicit.

**Why two engines.** The relational stores hold records whose shape is fixed
and whose integrity matters — an audit row must have exactly the columns it
has, and must not change. The profile document holds an evolving aggregate
that will accumulate new signal categories over time (bank cash flow, rent
reporting, employment tenure). Adding a field to a document requires no
migration; adding one to an audit table does. The choice follows the shape of
the data rather than a top-down mandate.

---

## 2 — Entity relationships

### `incomeverification` database

```mermaid
erDiagram
    PAYROLL_EVENT_RECORD {
        int8 id PK "surrogate key, sequence-generated"
        varchar user_id "worker identifier, logical join key"
        varchar employer_name "employer of record"
        date pay_period_start "first day of period"
        date pay_period_end "last day of period"
        float8 gross_pay "USD before deductions"
        float8 net_pay "USD after deductions, basis of scoring"
        varchar pay_frequency "WEEKLY BIWEEKLY SEMIMONTHLY MONTHLY"
        varchar source_provider "originating payroll provider"
        timestamp received_at "server-side ingest time"
    }

    INCOME_VERIFICATION_RESULT {
        varchar user_id PK "one row per worker"
        float8 average_monthly_income "mean monthly-equivalent net pay"
        float8 income_confidence_score "clamp(1 - CV, 0, 1)"
        varchar income_stability_label "STABLE MODERATE VOLATILE INSUFFICIENT_DATA"
        int4 pay_events_considered "window size, capped at 12"
        timestamp last_updated "last recomputation"
    }

    PAYROLL_EVENT_RECORD ||--|| INCOME_VERIFICATION_RESULT : "aggregated into"
```

The relationship is computed, not declarative. There is no foreign key: the
result row is derived by aggregating event rows sharing a `user_id`, and is
recomputed in full on every new event rather than incrementally maintained.

### `decisions` database

```mermaid
erDiagram
    ELIGIBILITY_DECISION {
        varchar decision_id PK "UUID, one per decision"
        varchar user_id "worker identifier, not unique"
        varchar decision "APPROVED REVIEW DENIED"
        float8 credit_limit_usd "recommended limit"
        float8 suggested_apr "recommended rate"
        float8 average_monthly_income "input signal, snapshotted"
        float8 income_confidence_score "input signal, snapshotted"
        varchar income_stability_label "input signal, snapshotted"
        varchar reasoning "plain-language justification"
        timestamp decided_at "when the rules were evaluated"
    }
```

A single table, deliberately. Note that `user_id` is **not** unique: a worker
accumulates one row per assessment. The most recent decision is the row with
the greatest `decided_at` for that `user_id`.

The three input signal columns are denormalised copies of values that also
exist in `income_verification_result`. This is intentional and load-bearing —
see [section 6](#6--data-lineage).

### Cross-database relationship

```mermaid
erDiagram
    INCOME_VERIFICATION_RESULT ||--o{ ELIGIBILITY_DECISION : "user_id"
    INCOME_VERIFICATION_RESULT ||--|| CREDIT_PROFILE : "user_id"

    INCOME_VERIFICATION_RESULT {
        varchar user_id PK "PostgreSQL incomeverification"
    }
    ELIGIBILITY_DECISION {
        varchar user_id "PostgreSQL decisions"
    }
    CREDIT_PROFILE {
        string _id PK "MongoDB creditprofile"
    }
```

`user_id` is the join key across all three stores, but the relationship is
**logical only**. No foreign key exists, and none could — the stores are
separate databases on two different engines. Referential integrity across
service boundaries is maintained by the event flow, not by the database.

This is a deliberate trade of enforced integrity for independent
deployability. The consequence is real and should be stated plainly: it is
possible for a decision row to exist for a `user_id` that has no
corresponding profile document, if the credit profile service was down when
the event was published. Kafka's retention means the consumer catches up when
it restarts, so the window is bounded, but it is not zero.

---

## 3 — Explicit DDL

The relational schema is currently generated by Hibernate from the JPA entity
classes. Generated schema is convenient during development but implicit — it
is not visible in the repository, and it changes when an annotation changes.

The DDL below makes the current structure explicit for review. It is held in
[`schema/incomeverification.sql`](schema/incomeverification.sql) and
[`schema/decisions.sql`](schema/decisions.sql).

```sql
-- database: incomeverification

CREATE TABLE payroll_event_record (
    id                BIGSERIAL         PRIMARY KEY,
    user_id           VARCHAR(128)      NOT NULL,
    employer_name     VARCHAR(255),
    pay_period_start  DATE              NOT NULL,
    pay_period_end    DATE              NOT NULL,
    gross_pay         DOUBLE PRECISION  NOT NULL,
    net_pay           DOUBLE PRECISION  NOT NULL,
    pay_frequency     VARCHAR(32),
    source_provider   VARCHAR(64),
    received_at       TIMESTAMP         NOT NULL
);

CREATE TABLE income_verification_result (
    user_id                  VARCHAR(128)      PRIMARY KEY,
    average_monthly_income   DOUBLE PRECISION  NOT NULL,
    income_confidence_score  DOUBLE PRECISION  NOT NULL,
    income_stability_label   VARCHAR(32)       NOT NULL,
    pay_events_considered    INTEGER           NOT NULL,
    last_updated             TIMESTAMP         NOT NULL
);
```

```sql
-- database: decisions

CREATE TABLE eligibility_decision (
    decision_id              VARCHAR(64)       PRIMARY KEY,
    user_id                  VARCHAR(128)      NOT NULL,
    decision                 VARCHAR(32)       NOT NULL,
    credit_limit_usd         DOUBLE PRECISION  NOT NULL,
    suggested_apr            DOUBLE PRECISION  NOT NULL,
    average_monthly_income   DOUBLE PRECISION  NOT NULL,
    income_confidence_score  DOUBLE PRECISION  NOT NULL,
    income_stability_label   VARCHAR(32)       NOT NULL,
    reasoning                VARCHAR(2000)     NOT NULL,
    decided_at               TIMESTAMP         NOT NULL
);
```

### Verifying this against the live database

Generated DDL and hand-written DDL diverge easily. Confirm before citing this
document:

```bash
./docs/data-model/schema/verify-schema.sh
```

The script dumps the live schema from both databases and prints it for
comparison. If the output differs from the DDL above, the DDL above is wrong
and should be corrected — the database is the source of truth.

---

## 4 — Document schema

### `credit_profiles` collection

```json
{
  "_id": "user-1001",
  "averageMonthlyIncome": 5306.70,
  "incomeConfidenceScore": 1.0,
  "incomeStabilityLabel": "STABLE",
  "payEventsConsidered": 4,
  "bureauScore": 598,
  "bureauSource": "Experian",
  "thinFileClassification": "STANDARD",
  "incomeHistory": [
    {
      "averageMonthlyIncome": 5306.70,
      "incomeConfidenceScore": 0.4,
      "incomeStabilityLabel": "INSUFFICIENT_DATA",
      "payEventsConsidered": 1,
      "observedAt": "2026-07-13T22:54:52.828Z"
    }
  ],
  "lastUpdated": "2026-07-13T23:03:58.403Z",
  "createdAt": "2026-07-13T22:54:53.017Z"
}
```

`_id` is the `userId` directly rather than a generated ObjectId. This gives
idempotent upserts by worker with no secondary index required, and makes the
document addressable by the same key used everywhere else in the platform.

`bureauScore` and `bureauSource` are nullable, and their nullity is the
defining condition of the thin-file case — not an error state.

`incomeHistory` is an unbounded array appended to on every consumed event. See
[section 8](#8--known-gaps) for the growth implication.

### Schema validator

MongoDB accepts documents of any shape by default. The validator in
[`schema/creditprofile.schema.json`](schema/creditprofile.schema.json) makes
the expected structure explicit and enforceable:

```javascript
db.runCommand({
  collMod: "credit_profiles",
  validator: { $jsonSchema: /* contents of creditprofile.schema.json */ },
  validationLevel: "moderate",
  validationAction: "warn"
});
```

`validationAction: "warn"` logs violations without rejecting writes, which is
the appropriate setting to introduce validation against an existing
collection. Move to `"error"` once the log is clean.

---

## 5 — Data dictionary

Meaning rather than type. Types are in [section 3](#3--explicit-ddl).

### Identifiers

| Field | Meaning |
|---|---|
| `user_id` / `_id` | Worker identifier assigned by the calling system. Pseudonymous — the platform holds no name, SSN, address, or account number, so this value is not directly identifying on its own. It is the join key across all three stores. |
| `decision_id` | UUID generated per decision. Not derived from `user_id`; two decisions for the same worker share nothing. |

### Income signals

| Field | Meaning |
|---|---|
| `gross_pay` | Earnings before deductions. Retained for completeness; not currently used in scoring. |
| `net_pay` | Earnings after deductions. **This is the value scoring operates on**, because disposable income is what services debt. |
| `pay_frequency` | Determines the multiplier converting this period's `net_pay` to a monthly equivalent: `WEEKLY` × 4.33, `BIWEEKLY` × 2.166, `SEMIMONTHLY` × 2.0, `MONTHLY` × 1.0. |
| `average_monthly_income` | Mean of the monthly-equivalent values across the window. Not a sum, and not annualised. |
| `income_confidence_score` | `clamp(1 − stddev/mean, 0, 1)` over the window. Measures **predictability, not magnitude** — a worker earning USD 2,000 every period scores higher than one averaging USD 8,000 with wide swings. |
| `income_stability_label` | Categorical band over the score. Exists so consumers can branch on a stable enum rather than hard-coding thresholds that may be retuned. |
| `pay_events_considered` | Window size actually used, capped at 12. Below 3, the rules engine will not approve regardless of score. |

### Decision outputs

| Field | Meaning |
|---|---|
| `decision` | `APPROVED`, `REVIEW`, or `DENIED`. `REVIEW` is a referral to a human underwriter, not a soft denial. |
| `credit_limit_usd` | 30% of `average_monthly_income` for approvals, 10% for referrals, 0 for denials. |
| `suggested_apr` | 18.99 for stable approvals, 22.99 for moderate, 24.99 for referrals, 0 for denials. Advisory; the consuming lender sets the actual rate. |
| `reasoning` | Plain-language statement of which rules fired and why. Written for the affected consumer, not for an engineer — this field is the raw material for an adverse action notice under Regulation B. |
| `decided_at` | Evaluation time. Combined with the append-only design, this is what makes the table a time series. |

### Profile aggregate

| Field | Meaning |
|---|---|
| `bureauScore` | Conventional credit score, or `null` where the bureau holds no file. Prototype uses a deterministic stub. |
| `bureauSource` | Which bureau responded. `null` alongside a `null` score. |
| `thinFileClassification` | `THIN_FILE` when the bureau returned nothing; `RICH_FILE` at score ≥ 720 with `STABLE` income; `STANDARD` otherwise. |
| `incomeHistory[]` | Append-only sequence of income assessment snapshots. Shows how confidence strengthened as pay history accumulated. |

---

## 6 — Data lineage

A single value, traced from arrival to every place it comes to rest.

```mermaid
flowchart LR
    A["PayrollEvent.netPay<br/>2450.00"] --> B["payroll_event_record.net_pay<br/>persisted verbatim"]
    B --> C["normalize by frequency<br/>2450.00 × 2.166 = 5306.70<br/>computed, not stored"]
    C --> D["income_verification_result<br/>.average_monthly_income"]
    D --> E["IncomeVerifiedEvent<br/>.averageMonthlyIncome"]
    E --> F["eligibility_decision<br/>.average_monthly_income"]
    E --> G["credit_profiles<br/>.averageMonthlyIncome"]
    G --> H["credit_profiles<br/>.incomeHistory[].averageMonthlyIncome"]
```

The same figure ends up stored in four places. That is deliberate
denormalisation, and the reason matters.

`eligibility_decision` stores the input signals alongside the output **so that
a decision can be re-derived without joining to a table that may since have
changed**. `income_verification_result` holds one mutable row per worker,
overwritten on every new pay event. If the audit record referenced it rather
than copying it, then reconstructing why a decision was made six months ago
would read today's income figure — and produce a different answer.

Basel II model risk management requires model outputs to be independently
reproducible. Regulation B requires that the reason given to a declined
consumer be the reason that actually applied. Neither is satisfiable if the
inputs are not frozen at decision time. The redundancy is the control.

---

## 7 — Privacy classification

The prototype holds no direct identifiers: no name, no Social Security number,
no address, no date of birth, no account or card number. `user_id` is an
opaque token supplied by the calling system.

That is a design property worth preserving. It means the platform can be
demonstrated, load-tested, and inspected publicly without exposing anyone.

| Field | Classification | Notes |
|---|---|---|
| `user_id` | Pseudonymous identifier | Not identifying alone; becomes identifying if joined to the caller's own records |
| `employer_name` | Indirectly identifying | Combined with income and period, could narrow to an individual in a small employer |
| `gross_pay`, `net_pay` | NPI under GLBA | Nonpublic personal financial information |
| `average_monthly_income` | NPI under GLBA | Derived, same classification as its inputs |
| `income_confidence_score` | Derived NPI | Not disclosed to third parties in the current design |
| `bureauScore`, `bureauSource` | FCRA-regulated consumer report data | Would require permissible purpose and FCRA compliance in production |
| `decision`, `reasoning` | Adverse action data under Regulation B | Must be retained and must be disclosable to the consumer |

### Regulatory implications for a production deployment

- **GLBA Safeguards Rule** — financial institutions must maintain administrative, technical and physical safeguards for NPI. Encryption at rest is not yet implemented; see [section 8](#8--known-gaps).
- **FCRA** — the moment a real bureau adapter replaces the stub, the platform becomes a user of consumer reports and inherits permissible purpose, accuracy, and dispute obligations.
- **Regulation B / ECOA** — adverse action notices must state the principal reason for denial. The `reasoning` field exists for this and must be retained for 25 months for consumer credit.
- **CCPA** — California residents may request deletion. The append-only design of `eligibility_decision` conflicts with deletion on request; reconciling the two requires a documented approach (crypto-shredding, or asserting the legal-obligation exemption for records retained under Regulation B).

That last tension is genuine and unresolved in this prototype. It is recorded
here rather than glossed over, because a reviewer with financial-services
background will look for it.

---

## 8 — Known gaps

Stated plainly so the model is not read as more complete than it is.

### Missing indexes

Only primary keys are indexed. Two query patterns run without index support:

```sql
-- decision-service: most recent decision for a worker
SELECT * FROM eligibility_decision WHERE user_id = ? ORDER BY decided_at DESC;

-- income-verification-service: rolling window of pay events
SELECT * FROM payroll_event_record WHERE user_id = ? ORDER BY pay_period_start DESC LIMIT 12;
```

Both are sequential scans today. At demonstration volumes this is invisible;
under the read-path load scenario in `perf/` it should become measurable, and
it is a plausible explanation if read latency degrades with accumulated data.

Recommended, in [`schema/recommended-indexes.sql`](schema/recommended-indexes.sql):

```sql
CREATE INDEX idx_eligibility_decision_user_decided
    ON eligibility_decision (user_id, decided_at DESC);

CREATE INDEX idx_payroll_event_user_period
    ON payroll_event_record (user_id, pay_period_start DESC);
```

These are proposed rather than applied. Applying them before running the
performance suite would remove the opportunity to measure the difference, so
the sequence matters: measure, apply, measure again, report both.

### Missing constraints

The enum columns (`pay_frequency`, `income_stability_label`, `decision`) are
plain `VARCHAR` with no `CHECK`. Validity is enforced only in application code.
A direct `INSERT` bypassing the service could write nonsense. Proposed
constraints are in the same file.

### Unbounded array growth

`credit_profiles.incomeHistory` grows by one element per consumed event with
no cap. A worker with five years of biweekly pay accumulates roughly 130
entries — acceptable. A load test appending thousands is not, and MongoDB's
16 MB document limit is a hard ceiling. A retention policy capping the array
at, say, the most recent 50 snapshots is the obvious mitigation, and is not
implemented.

### No retention policy

Nothing is ever deleted. Regulation B implies a 25-month floor for adverse
action records; there is no corresponding ceiling defined, and no archival or
purge job.

### No encryption at rest

PostgreSQL and MongoDB volumes are unencrypted. Acceptable for a prototype
holding synthetic data; not acceptable under the GLBA Safeguards Rule for real
NPI.

### No schema migration tooling

Schema is generated by Hibernate from entity annotations. There is no
Flyway or Liquibase migration history, so schema changes are neither
versioned nor reversible. The explicit DDL in this document is a first step
toward that, but it is documentation rather than a migration.

---

## Static exports

ER diagrams are rendered to [`rendered/`](rendered/) for contexts that cannot
display Mermaid. Regenerate with:

```bash
./render.sh
```
