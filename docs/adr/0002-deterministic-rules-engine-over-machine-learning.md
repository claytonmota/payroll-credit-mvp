# 0002 — Deterministic rules engine over machine learning

**Status:** Accepted
**Date:** 2026-07-10
**Affects:** `decision-service`

## Context

The eligibility decision — approve, refer for review, or decline, together
with a credit limit and a suggested rate — could be produced in three ways:

1. A machine learning classifier trained on historical loan performance.
2. A deterministic rules engine with explicit thresholds.
3. A hybrid, where a model produces features that rules then act on.

The empirical literature favours the first. FinRegLab's 2025 study *Advancing
the Credit Ecosystem: Machine Learning & Cash Flow Data in Consumer
Underwriting* compared logistic regression and machine learning models across
combinations of bureau data and cash-flow data, and found that adopting
machine learning together with cash-flow data produced the largest gains in
predictiveness — and, importantly for this platform's purpose, expanded credit
access without increasing lender default risk.

That finding is directly relevant here. A platform whose stated goal is to
extend credit access to thin-file consumers should care a great deal about any
approach that measurably expands access.

Against that sit two regulatory constraints that apply to any system producing
consumer credit decisions in the United States:

- **Regulation B**, implementing the Equal Credit Opportunity Act, requires
  that a consumer denied credit receive a statement of the *specific principal
  reasons* for the denial. A reason must be accurate and specific to that
  applicant.
- **Basel II model risk management** requires that a model's output be
  independently reproducible and auditable by someone other than its authors.

Neither constraint prohibits machine learning. Both are satisfiable with
sufficient investment — post-hoc explainability tooling, model documentation,
monitoring, validation practice. The question is what a prototype with one
engineer should build first.

There is also a data constraint specific to this stage: there is no historical
loan performance data. A supervised model has nothing to train on. Any model
here would be trained on synthetic data and would demonstrate the shape of a
solution rather than a working one.

## Decision

The `decision-service` implements a deterministic, rule-based engine. No
machine learning appears in the decision path.

Rules are evaluated in a fixed order and evaluation stops at the first match:

| Order | Condition | Outcome |
|---|---|---|
| 1 | Fewer than 3 pay events on record | `REVIEW` |
| 2 | Average monthly income below USD 1,800 | `DENIED` |
| 3 | Confidence score ≥ 0.75 and label `STABLE` | `APPROVED` |
| 4 | Confidence score ≥ 0.50 | `REVIEW` |
| 5 | Otherwise | `DENIED` |

Every decision persists a `reasoning` field: a plain-language statement of
which rules fired and the values that triggered them, written to be read by
the affected consumer rather than by an engineer.

Implementation: `decision-service/src/main/java/com/mota/decision/service/EligibilityRulesEngine.java`

## Consequences

**Adverse action notices are derivable directly from stored data.** The
`reasoning` field is the raw material for a Regulation B notice with no
additional inference step. There is no gap between what the system decided and
what the consumer can be told.

**Decisions are reproducible without the model.** Because the engine is
deterministic and the input signals are frozen into the audit record (see
[ADR-0003](0003-input-signals-denormalised-into-audit-record.md)), any past
decision can be re-derived and independently verified. This is what Basel II
model risk management asks for, satisfied structurally rather than through
tooling.

**The rules are unit-testable in full.** Every branch has a test. There is no
question of test coverage over a decision boundary.

**Predictive accuracy is almost certainly lower than an equivalent trained
model would achieve**, and the size of that gap is unknown. This is the real
cost, and it is not a small one to wave away: if a machine learning model
would approve creditworthy applicants that these rules decline, the cost falls
on exactly the population the platform exists to serve. Accepting this
trade-off is defensible for a prototype without performance data; it would
require re-examination before any production deployment, against measured
outcomes rather than assumption.

**The thresholds are unvalidated.** Values such as 0.75, 0.50, and USD 1,800
are reasoned but not calibrated — there is no performance data to calibrate
them against. They should be read as placeholders with plausible magnitudes,
not as tuned parameters.

**Recalibration has no defined mechanism.** A trained model is retrained. This
engine would need thresholds revised by hand, and there is currently no process
for deciding when or by how much.

## Revisiting this decision

The likely evolution is a hybrid rather than a reversal: machine learning
applied at the feature-engineering layer, producing signals that the
deterministic rules then act on. That preserves a fully explainable decision
path while allowing model-derived inputs.

The conditions that would justify revisiting are: accumulation of real loan
performance data sufficient to train and validate a model; and a measurement of
the access gap between the two approaches on the thin-file population
specifically, rather than on a general portfolio.

## References

- FinRegLab, *Advancing the Credit Ecosystem: Machine Learning & Cash Flow
  Data in Consumer Underwriting* (2025) — https://finreglab.org
- 12 CFR Part 1002 (Regulation B), adverse action notice requirements
- Basel Committee on Banking Supervision, Basel II framework, model risk
  management
