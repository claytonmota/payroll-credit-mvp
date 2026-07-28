# Architecture Decision Records

An Architecture Decision Record captures a single significant design choice,
the context that forced it, and the consequences accepted in making it.

They exist because the reasoning behind a decision decays faster than the code
that implements it. Six months later the code still shows *what* was done; only
a record shows *why*, and what was given up. Without that, a future maintainer
either preserves a constraint that no longer applies or removes a safeguard
without knowing it was one.

These records are the single source of truth for the decisions they cover. The
System Architecture Document and the OpenAPI specifications reference them
rather than restating them.

## Records

| # | Title | Status | Date |
|---|---|---|---|
| [0001](0001-kafka-producers-omit-type-headers.md) | Kafka producers omit type headers | Accepted | 2026-07-13 |
| [0002](0002-deterministic-rules-engine-over-machine-learning.md) | Deterministic rules engine over machine learning | Accepted | 2026-07-10 |
| [0003](0003-input-signals-denormalised-into-audit-record.md) | Input signals denormalised into the audit record | Accepted | 2026-07-10 |
| [0004](0004-polyglot-persistence.md) | Polyglot persistence — PostgreSQL and MongoDB | Accepted | 2026-07-08 |

## Format

Each record follows the structure proposed by Michael Nygard:

- **Status** — Proposed, Accepted, Deprecated, or Superseded by ADR-NNNN
- **Context** — the forces at play, stated neutrally, without the answer
- **Decision** — what was chosen, in the active voice
- **Consequences** — what follows, including what became harder

The consequences section is the one that matters most and the one most often
written badly. A record listing only benefits is marketing, not engineering.
Every decision here gave something up, and each record says what.

## Conventions

- Records are immutable once accepted. A changed mind produces a new record
  that supersedes the old one; the original stays in the repository with its
  status updated.
- Numbering is sequential and never reused.
- Filenames are `NNNN-title-in-kebab-case.md`.
- A record covers one decision. If it needs "and also", it is two records.
