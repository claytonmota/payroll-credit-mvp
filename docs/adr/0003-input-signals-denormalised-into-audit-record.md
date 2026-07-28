# 0003 — Input signals denormalised into the audit record

**Status:** Accepted
**Date:** 2026-07-10
**Affects:** `decision-service`, `eligibility_decision` table

## Context

The `decision-service` produces an eligibility decision from three input
signals: average monthly income, income confidence score, and income stability
label. All three originate in the `income-verification-service` and arrive on
the `income.verified` Kafka event.

Those same three values are also persisted by the income verification service
in its own `income_verification_result` table — one row per worker, **updated
in place** on every incoming payroll event.

The audit record could therefore either reference those values or copy them.
Normalisation argues for referencing: the values exist elsewhere, storing them
twice is redundant, and redundant storage can diverge.

But `income_verification_result` holds current state, not history. A worker
who submits a new pay event next month overwrites the row. A decision record
that referenced it would, six months later, be read alongside an income figure
that had changed several times since the decision was made.

Two obligations make that unacceptable:

- **Regulation B** requires that the reason given to a declined applicant be
  the specific principal reason that actually applied to that application.
- **Basel II model risk management** requires that a model's output be
  independently reproducible.

Reproducing a decision means re-deriving it from the inputs that produced it.
If the inputs are mutable and unversioned, the decision is not reproducible in
any meaningful sense — re-running the rules would produce a different answer
and there would be no way to tell which was right.

## Decision

`eligibility_decision` stores the input signals alongside the output, as
columns on the decision row itself:

```
average_monthly_income   DOUBLE PRECISION  NOT NULL
income_confidence_score  DOUBLE PRECISION  NOT NULL
income_stability_label   VARCHAR(255)      NOT NULL
```

The table is append-only. A row is never updated; a changed assessment inserts
a new row with a new `decision_id`.

Together these mean every decision carries a frozen snapshot of everything
that produced it.

## Consequences

**Any past decision can be re-derived and independently verified.** Feed the
stored signals back through the rules engine and the stored outcome must
result. That property holds indefinitely and does not depend on any other
table.

**The redundancy is the control, not a defect.** A reviewer noticing that the
same value appears in two tables should read it as deliberate: the copy in the
audit record is immutable evidence, the row in `income_verification_result` is
mutable working state. They serve different purposes and are expected to
diverge as new pay events arrive.

**Storage grows with decision count rather than with worker count.** Each
assessment writes a full row rather than a reference. At the scale this
platform targets the cost is negligible; at very high volume it would warrant
review.

**A schema change to the income signals requires care.** Adding a fourth
signal means adding a column here too, and historical rows will not have it.
Any query spanning old and new decisions must tolerate that.

**Deletion under CCPA becomes difficult.** A California resident's right to
request deletion sits awkwardly against an append-only audit table that
Regulation B requires be retained for 25 months for consumer credit. This
tension is unresolved in the prototype and is recorded as a known gap in
`docs/data-model/README.md`. Resolving it would mean either crypto-shredding
or asserting the legal-obligation exemption — a decision that needs counsel,
not engineering.
