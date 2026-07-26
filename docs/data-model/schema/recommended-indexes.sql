-- =============================================================================
-- Recommended hardening — PROPOSED, NOT APPLIED
--
-- Nothing in this file is currently present in the running system. It is a
-- documented proposal, kept alongside the schema so that the gap between what
-- exists and what should exist is explicit rather than implicit.
--
-- Sequencing note: applying the indexes below BEFORE running the performance
-- suite in perf/ would remove the opportunity to measure their effect. The
-- useful order is: measure, apply, measure again, report both figures.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- Indexes
--
-- Only primary keys are indexed today. The two hot query patterns below both
-- run as sequential scans. Invisible at demonstration volumes; expected to
-- become measurable under the read-path load scenario, and a plausible
-- explanation if read latency degrades as data accumulates.
-- -----------------------------------------------------------------------------

-- database: decisions
--
-- Supports: SELECT * FROM eligibility_decision
--           WHERE user_id = ? ORDER BY decided_at DESC
--
-- Composite with descending timestamp so that both the "current decision"
-- lookup and the full-history retrieval are served by one index without a sort.
CREATE INDEX idx_eligibility_decision_user_decided
    ON eligibility_decision (user_id, decided_at DESC);


-- database: incomeverification
--
-- Supports: SELECT * FROM payroll_event_record
--           WHERE user_id = ? ORDER BY pay_period_start DESC LIMIT 12
--
-- This is the rolling-window read executed on every incoming payroll event,
-- so it sits directly in the write path of the pipeline.
CREATE INDEX idx_payroll_event_user_period
    ON payroll_event_record (user_id, pay_period_start DESC);


-- -----------------------------------------------------------------------------
-- Check constraints
--
-- The enum-valued columns are plain VARCHAR with validity enforced only in
-- application code. A direct INSERT bypassing the service could write a value
-- the rules engine has no branch for.
-- -----------------------------------------------------------------------------

-- database: incomeverification
ALTER TABLE payroll_event_record
    ADD CONSTRAINT chk_pay_frequency
    CHECK (pay_frequency IN ('WEEKLY', 'BIWEEKLY', 'SEMIMONTHLY', 'MONTHLY'));

ALTER TABLE payroll_event_record
    ADD CONSTRAINT chk_pay_non_negative
    CHECK (gross_pay >= 0 AND net_pay >= 0);

ALTER TABLE payroll_event_record
    ADD CONSTRAINT chk_pay_period_ordered
    CHECK (pay_period_end >= pay_period_start);

ALTER TABLE income_verification_result
    ADD CONSTRAINT chk_stability_label
    CHECK (income_stability_label IN
           ('STABLE', 'MODERATE', 'VOLATILE', 'INSUFFICIENT_DATA'));

ALTER TABLE income_verification_result
    ADD CONSTRAINT chk_confidence_range
    CHECK (income_confidence_score >= 0 AND income_confidence_score <= 1);

ALTER TABLE income_verification_result
    ADD CONSTRAINT chk_events_considered
    CHECK (pay_events_considered >= 0 AND pay_events_considered <= 12);


-- database: decisions
ALTER TABLE eligibility_decision
    ADD CONSTRAINT chk_decision_value
    CHECK (decision IN ('APPROVED', 'REVIEW', 'DENIED'));

ALTER TABLE eligibility_decision
    ADD CONSTRAINT chk_decision_stability_label
    CHECK (income_stability_label IN
           ('STABLE', 'MODERATE', 'VOLATILE', 'INSUFFICIENT_DATA'));

ALTER TABLE eligibility_decision
    ADD CONSTRAINT chk_decision_confidence_range
    CHECK (income_confidence_score >= 0 AND income_confidence_score <= 1);

ALTER TABLE eligibility_decision
    ADD CONSTRAINT chk_limit_non_negative
    CHECK (credit_limit_usd >= 0 AND suggested_apr >= 0);

-- A denial must carry no credit line. Encodes a rule the engine already
-- enforces, at the level where it cannot be bypassed.
ALTER TABLE eligibility_decision
    ADD CONSTRAINT chk_denied_has_no_limit
    CHECK (decision <> 'DENIED' OR credit_limit_usd = 0);
