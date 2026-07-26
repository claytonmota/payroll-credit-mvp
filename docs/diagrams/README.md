# Processing Flows

Sequence and structural diagrams for the Payroll-Integrated Real-Time Credit
Infrastructure.

These diagrams are written in [Mermaid](https://mermaid.js.org) and render
directly in GitHub. They are text, not images, so they are version-controlled
alongside the code and cannot silently drift out of date the way an exported
PNG can.

They complement rather than replace the architecture overview in
`docs/architecture-v4.png`: that diagram shows *what the system is made of*,
while these show *what happens over time* when it runs.

| # | Diagram | Question it answers |
|---|---|---|
| 1 | [End-to-end processing flow](#1--end-to-end-processing-flow) | What happens between a payroll event arriving and a credit decision existing? |
| 2 | [Thin-file classification](#2--thin-file-classification) | How does a worker with no bureau history get assessed? |
| 3 | [Cross-language fan-out](#3--cross-language-fan-out) | How can a C# service and a Java service consume the same event? |
| 4 | [Assessment evolution](#4--assessment-evolution-and-the-audit-trail) | How does confidence build as pay history accumulates? |
| 5 | [Deployment topology](#5--deployment-topology) | What actually runs where? |

---

## 1 — End-to-end processing flow

The core path. A payroll provider submits one pay period; the platform
normalizes it, recomputes the worker's income confidence score, and fans the
result out to two independent consumers that produce an eligibility decision
and an aggregate credit profile.

The synchronous portion ends at step 4. Everything after that is asynchronous:
the caller has already received its response and the pipeline continues in the
background.

```mermaid
sequenceDiagram
    autonumber
    actor Provider as Payroll Provider
    participant Ing as ingestion-service
    participant T1 as Kafka payroll.events
    participant Inc as income-verification-service
    participant PgI as PostgreSQL incomeverification
    participant T2 as Kafka income.verified
    participant Dec as decision-service
    participant PgD as PostgreSQL decisions
    participant Prof as credit-profile-service
    participant Mon as MongoDB creditprofile

    rect rgb(235, 245, 255)
        Note over Provider, Ing: Synchronous
        Provider->>Ing: POST /v1/payroll/events
        Note right of Provider: netPay 2450.00, BIWEEKLY
        Ing->>T1: publish PayrollEvent
        T1-->>Ing: ack, acks=all
        Ing-->>Provider: 202 Accepted
    end

    T1->>Inc: consume PayrollEvent
    Inc->>PgI: INSERT payroll_event_record
    Inc->>PgI: SELECT rolling window, max 12 events
    PgI-->>Inc: pay history

    Note over Inc: normalize net pay to monthly equivalent<br/>CV = stddev / mean<br/>score = clamp(1 - CV, 0, 1)

    Inc->>PgI: UPSERT income_verification_result
    Inc->>T2: publish IncomeVerifiedEvent

    par consumer group: decision-service
        T2->>Dec: consume IncomeVerifiedEvent
        Note over Dec: deterministic rules engine<br/>no machine learning
        Dec->>PgD: INSERT eligibility_decision
        Note right of PgD: append-only, never updated
    and consumer group: credit-profile-service
        T2->>Prof: consume IncomeVerifiedEvent
        Prof->>Prof: bureau lookup
        Prof->>Mon: upsert credit_profiles document
    end
```

Reading the results is an ordinary synchronous query against whichever service
owns the answer:

```mermaid
sequenceDiagram
    autonumber
    actor Lender
    participant Inc as income-verification-service
    participant Dec as decision-service
    participant Prof as credit-profile-service

    Lender->>Inc: GET /v1/income/verification/user-1001
    Inc-->>Lender: 200, score 1.0, STABLE

    Lender->>Dec: GET /v1/eligibility/user-1001
    Dec-->>Lender: 200, APPROVED, limit 1592.01, APR 18.99

    Lender->>Prof: GET /v1/credit-profile/user-1001
    Prof-->>Lender: 200, STANDARD, bureau 598

    Note over Lender, Dec: A 404 immediately after submitting an event<br/>is expected and transient. Poll briefly.
```

---

## 2 — Thin-file classification

The financial inclusion case, and the reason the platform exists.

A worker with verified, stable payroll income but no credit bureau file is
invisible to conventional underwriting — there is no score to decline them on,
so they are declined for absence of data. This flow shows how the platform
identifies that population as a distinct, assessable segment rather than
discarding it.

Any `userId` ending in `-thinfile` deterministically produces this path in the
prototype, so the case can be demonstrated on demand.

```mermaid
sequenceDiagram
    autonumber
    participant T2 as Kafka income.verified
    participant Prof as credit-profile-service
    participant Bureau as bureau lookup adapter
    participant Mon as MongoDB
    actor Lender

    T2->>Prof: IncomeVerifiedEvent for user-9999-thinfile
    Note right of T2: averageMonthlyIncome 4766.00<br/>score 1.0, STABLE

    Prof->>Bureau: lookup by userId
    Note over Bureau: prototype: deterministic in-process stub<br/>production: Experian, Equifax or TransUnion
    Bureau-->>Prof: no file on record

    alt bureau returned no history
        Note over Prof: THIN_FILE
    else score at least 720 and income STABLE
        Note over Prof: RICH_FILE
    else
        Note over Prof: STANDARD
    end

    Prof->>Mon: upsert profile, bureauScore null
    Mon-->>Prof: acknowledged

    Lender->>Prof: GET /v1/credit-profile/user-9999-thinfile
    Prof->>Mon: find by _id
    Mon-->>Prof: document
    Prof-->>Lender: 200, THIN_FILE, bureauScore null

    Note over Lender: Verified income, no bureau file.<br/>Assessable on payroll evidence alone.
```

---

## 3 — Cross-language fan-out

Three services in this platform are Java on Spring Boot; the credit profile
service is C# on .NET 8. They consume the same Kafka topic, in independent
consumer groups, with no shared library and no generated stubs between them.

That interoperability is not automatic. Spring Kafka's `JsonSerializer` emits
a `__TypeId__` header naming the *producer's* Java class, and a consumer
configured with `JsonDeserializer` will refuse any class outside its trusted
package list. The result is that a service in a different package — let alone
a different language — cannot deserialize the message at all.

ADR-001 resolves this by requiring every producer to set
`ADD_TYPE_INFO_HEADERS = false`. The event schema becomes the contract; the
producing class name becomes an implementation detail.

```mermaid
sequenceDiagram
    autonumber
    participant Inc as income-verification-service (Java)
    participant T as Kafka income.verified
    participant Dec as decision-service (Java)
    participant Prof as credit-profile-service (C# .NET 8)

    rect rgb(255, 240, 240)
        Note over Inc, Prof: Before ADR-001 — default Spring Kafka behaviour
        Inc->>T: publish, header __TypeId__ = com.mota.incomeverification.model.IncomeVerifiedEvent
        T->>Dec: deliver
        Note over Dec: trusted packages: com.mota.decision.model
        Dec--xDec: IllegalArgumentException<br/>class not in trusted packages
        T->>Prof: deliver
        Note over Prof: no Java class loader exists here
        Prof--xProf: header is meaningless
    end

    rect rgb(235, 250, 240)
        Note over Inc, Prof: After ADR-001 — ADD_TYPE_INFO_HEADERS = false
        Inc->>T: publish, JSON body only, no type header
        T->>Dec: deliver
        Note over Dec: deserialize into<br/>com.mota.decision.model.IncomeVerifiedEvent
        Dec-->>Dec: decision persisted
        T->>Prof: deliver
        Note over Prof: deserialize into<br/>CreditProfileService.Models.IncomeVerifiedEvent
        Prof-->>Prof: profile persisted
    end

    Note over Inc, Prof: A future consumer in Python, Go or Rust joins<br/>the same topic with no change to the producer.
```

The trade-off is deliberate: consumers must know their target type at compile
time, since there is no type resolution from headers. In a system where each
service owns its own data transfer objects, that is the desired property — it
forces explicit awareness of the contract rather than implicit coupling to
another service's internals.

---

## 4 — Assessment evolution and the audit trail

A worker's assessment is not a single computation. It is recomputed on every
incoming pay event, and each recomputation appends a new immutable decision
record. The history is therefore a complete account of how the platform's view
of that worker changed as evidence accumulated.

The stability label follows from the confidence score and the number of events
on record:

```mermaid
stateDiagram-v2
    direction LR
    [*] --> INSUFFICIENT_DATA: first pay event received

    INSUFFICIENT_DATA --> VOLATILE: 2 or more events, score below 0.60
    INSUFFICIENT_DATA --> MODERATE: 2 or more events, score 0.60 to 0.84
    INSUFFICIENT_DATA --> STABLE: 2 or more events, score 0.85 or above

    VOLATILE --> MODERATE: earnings steady, score rises
    MODERATE --> STABLE: earnings steady, score rises
    STABLE --> MODERATE: earnings fluctuate, score falls
    MODERATE --> VOLATILE: earnings fluctuate, score falls

    note right of STABLE
        score = clamp(1 - CV, 0, 1)
        CV = stddev / mean
        over a rolling window of up to 12 events
    end note
```

Each recomputation produces a new decision. Nothing is overwritten:

```mermaid
sequenceDiagram
    autonumber
    actor Provider as Payroll Provider
    participant Inc as income-verification-service
    participant Dec as decision-service
    participant PgD as eligibility_decision

    Provider->>Inc: pay event 1
    Inc->>Dec: score 0.40, INSUFFICIENT_DATA, 1 event
    Note over Dec: rule 1 — fewer than 3 events
    Dec->>PgD: INSERT decision REVIEW

    Provider->>Inc: pay event 2
    Inc->>Dec: score 1.00, STABLE, 2 events
    Note over Dec: rule 1 still applies
    Dec->>PgD: INSERT decision REVIEW

    Provider->>Inc: pay event 3
    Inc->>Dec: score 1.00, STABLE, 3 events
    Note over Dec: rule 3 — confidence and stability met
    Dec->>PgD: INSERT decision APPROVED

    Provider->>Inc: pay event 4
    Inc->>Dec: score 1.00, STABLE, 4 events
    Dec->>PgD: INSERT decision APPROVED

    Note over PgD: 4 rows, none updated.<br/>GET /v1/eligibility/{id}/history returns all four.
```

This matters for two reasons beyond good practice. Regulation B requires that
a declined consumer receive the specific principal reason for the decision, so
the `reasoning` recorded alongside each outcome must be the one that actually
applied at that moment. And Basel II model risk management requires that a
model's output be independently reproducible — which is only meaningful if the
inputs that produced it were retained.

---

## 5 — Deployment topology

What physically runs, and where. The entire platform currently occupies a
single EC2 instance; this is a deliberate choice for the prototype phase,
maximising reproducibility and ease of inspection at the cost of availability.

```mermaid
flowchart TB
    consumer["API consumer<br/>browser, curl, Postman"]

    subgraph aws["AWS EC2 m7i-flex.large, 2 vCPU / 8 GB, us-east-1, Elastic IP 3.229.114.98"]
        nginx["nginx<br/>TLS termination, subdomain routing"]

        subgraph services["Application containers"]
            ing["ingestion-service<br/>Java, :8081"]
            inc["income-verification-service<br/>Java, :8082"]
            dec["decision-service<br/>Java, :8083"]
            prof["credit-profile-service<br/>C# .NET 8, :8084"]
        end

        subgraph infra["Infrastructure containers, not publicly exposed"]
            kafka["Kafka + ZooKeeper"]
            pg[("PostgreSQL<br/>incomeverification, decisions")]
            mongo[("MongoDB<br/>creditprofile")]
        end
    end

    consumer -->|HTTPS| nginx
    nginx --> ing
    nginx --> inc
    nginx --> dec
    nginx --> prof

    ing -->|produce payroll.events| kafka
    kafka -->|consume payroll.events| inc
    inc -->|produce income.verified| kafka
    kafka -->|consume income.verified| dec
    kafka -->|consume income.verified| prof

    inc --> pg
    dec --> pg
    prof --> mongo
```

Four subdomains resolve to the same Elastic IP and are separated by nginx
virtual host:

| Subdomain | Container port |
|---|---|
| `ingestion.payroll-credit.com` | 8081 |
| `income.payroll-credit.com` | 8082 |
| `decision.payroll-credit.com` | 8083 |
| `creditprofile.payroll-credit.com` | 8084 |

Kafka, ZooKeeper, PostgreSQL and MongoDB have no published ports on the
security group. They are reachable only from within the Docker network.

The scaling path out of this topology — partition count, managed data
services, replica counts — is set out in section 11 of the System Architecture
Document.

---

## Static exports

GitHub renders the blocks above natively, so no build step is needed for
normal use. Static PNG exports are kept in [`rendered/`](rendered/) for the
contexts where Mermaid cannot render — PDF submissions, slide decks, printed
material.

| File | Diagram |
|---|---|
| [`01-end-to-end-processing-flow.png`](rendered/01-end-to-end-processing-flow.png) | End-to-end processing flow |
| [`02-reading-results.png`](rendered/02-reading-results.png) | Reading results |
| [`03-thin-file-classification.png`](rendered/03-thin-file-classification.png) | Thin-file classification |
| [`04-cross-language-fanout.png`](rendered/04-cross-language-fanout.png) | Cross-language fan-out |
| [`05-income-stability-states.png`](rendered/05-income-stability-states.png) | Income stability state transitions |
| [`06-audit-trail-accumulation.png`](rendered/06-audit-trail-accumulation.png) | Audit trail accumulation |
| [`07-deployment-topology.png`](rendered/07-deployment-topology.png) | Deployment topology |

To regenerate them after editing this file:

```bash
./render.sh
```

The script splits each fenced `mermaid` block out of this document and renders
it with the Mermaid CLI, installing the CLI on first run if it is absent. The
`NAMES` array inside the script must stay in the same order as the blocks
here; the script fails loudly if the counts diverge.
