# 0004 — Polyglot persistence: PostgreSQL and MongoDB

**Status:** Accepted
**Date:** 2026-07-08
**Affects:** all four services

## Context

The platform stores three kinds of data with different shapes and different
rates of change:

1. **Raw payroll events and the current income assessment** — fixed structure,
   defined by the canonical event contract, changing only when that contract
   changes.
2. **Eligibility decisions** — fixed structure, append-only, subject to
   regulatory retention and audit requirements.
3. **Aggregate credit profiles** — a composite of income signals, credit
   bureau data, a file classification, and a growing history of snapshots.
   This shape is expected to acquire new categories over time: bank cash-flow
   data, rent reporting, employment tenure.

A single engine could hold all three. PostgreSQL's JSONB would accommodate the
third; MongoDB would accommodate the first two. Using one engine would reduce
operational surface, the number of drivers, and the amount a new contributor
must learn.

## Decision

PostgreSQL holds the transactional and audit data, owned by
`income-verification-service` and `decision-service` in separate logical
databases. MongoDB holds the aggregate profile documents, owned by
`credit-profile-service`.

The choice follows the shape of the data. Records whose structure is fixed and
whose integrity is a regulatory concern go in a relational store, where a
column either exists or does not and a constraint can enforce it. The
aggregate, whose structure will change as signal categories are added, goes in
a document store, where adding a field requires no migration.

## Consequences

**Each service owns its schema outright.** No service reads another's tables.
The contract between them is the Kafka event, which is explicit and versionable,
rather than a shared schema, which couples deployments.

**Adding a signal category to the profile requires no migration.** A new field
appears on documents written after the change; older documents simply lack it.
For an aggregate whose composition is expected to grow, this is the property
that matters.

**Audit data gets relational guarantees where they count.** `NOT NULL` on
`reasoning` means an eligibility decision without a stated reason cannot be
written.

**No referential integrity exists across the stores.** `user_id` joins all
three logically, but nothing enforces it, and nothing could — they are separate
databases on two engines. A decision row can exist for a worker with no profile
document if the profile service was down when the event was published. Kafka
retention bounds the window, since the consumer catches up on restart, but the
window is not zero.

**Operational surface is larger.** Two engines to run, back up, monitor,
patch, and understand. On the current single-host deployment this is a
meaningful share of the memory footprint.

**Cross-store queries are application-level.** Any question spanning decisions
and profiles requires two round trips and a join in code. There is no way to
ask the databases a single question.

**A contributor must know both.** The .NET service uses MongoDB.Driver and the
Java services use Spring Data JPA. Familiarity with one does not transfer.
