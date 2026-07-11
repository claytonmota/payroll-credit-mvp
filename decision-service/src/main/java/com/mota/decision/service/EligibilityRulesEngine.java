package com.mota.decision.service;

import com.mota.decision.model.EligibilityDecision;
import com.mota.decision.model.IncomeVerifiedEvent;
import com.mota.decision.repository.EligibilityDecisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Transparent, rules-based eligibility engine — the "Rules Engine"
 * component of the Risk Orchestration Engine in the architecture
 * diagram.
 * <p>
 * Every decision records:
 * <ul>
 *   <li>The input income signals used (average monthly income,
 *       confidence score, stability label);</li>
 *   <li>The final categorical outcome (APPROVED, REVIEW, DENIED);</li>
 *   <li>The specific rules that fired, in human-readable form.</li>
 * </ul>
 * This "reasoning" field is what makes the decision auditable and
 * regulator-defensible (CFPB adverse action, Basel II model
 * transparency, ECOA reason codes).
 */
@Service
public class EligibilityRulesEngine {

    private static final Logger log = LoggerFactory.getLogger(EligibilityRulesEngine.class);

    // Thin-file / financial-inclusion policy thresholds. In production
    // these would be sourced from a config service and versioned.
    private static final double MIN_MONTHLY_INCOME_FOR_APPROVAL = 1800.0;
    private static final double MIN_CONFIDENCE_FOR_APPROVAL = 0.75;
    private static final double MIN_CONFIDENCE_FOR_REVIEW = 0.5;
    private static final int MIN_EVENTS_FOR_APPROVAL = 3;

    private final EligibilityDecisionRepository repository;

    public EligibilityRulesEngine(EligibilityDecisionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public EligibilityDecision evaluate(IncomeVerifiedEvent income) {
        List<String> reasons = new ArrayList<>();
        String outcome;
        double creditLimit;
        double apr;

        boolean insufficientData = "INSUFFICIENT_DATA".equals(income.getIncomeStabilityLabel())
                || income.getPayEventsConsidered() == null
                || income.getPayEventsConsidered() < MIN_EVENTS_FOR_APPROVAL;

        boolean incomeBelowFloor = income.getAverageMonthlyIncome() == null
                || income.getAverageMonthlyIncome() < MIN_MONTHLY_INCOME_FOR_APPROVAL;

        double confidence = income.getIncomeConfidenceScore() == null ? 0.0
                : income.getIncomeConfidenceScore();

        if (insufficientData) {
            outcome = "REVIEW";
            reasons.add("Insufficient payroll history: fewer than "
                    + MIN_EVENTS_FOR_APPROVAL + " pay events on record.");
        } else if (incomeBelowFloor) {
            outcome = "DENIED";
            reasons.add("Average monthly income ($" + income.getAverageMonthlyIncome()
                    + ") below minimum threshold ($" + MIN_MONTHLY_INCOME_FOR_APPROVAL + ").");
        } else if (confidence >= MIN_CONFIDENCE_FOR_APPROVAL
                && "STABLE".equals(income.getIncomeStabilityLabel())) {
            outcome = "APPROVED";
            reasons.add("Stable income stream with high confidence score (" + confidence + ").");
            reasons.add("Average monthly income meets threshold.");
        } else if (confidence >= MIN_CONFIDENCE_FOR_REVIEW) {
            outcome = "REVIEW";
            reasons.add("Moderate income confidence (" + confidence
                    + ") requires manual underwriter review.");
        } else {
            outcome = "DENIED";
            reasons.add("Income confidence score (" + confidence + ") below review threshold ("
                    + MIN_CONFIDENCE_FOR_REVIEW + ").");
        }

        // Credit limit sizing: 30% of average monthly income for APPROVED,
        // conservative fraction for REVIEW, zero for DENIED.
        if ("APPROVED".equals(outcome)) {
            creditLimit = round2(income.getAverageMonthlyIncome() * 0.30);
            apr = "STABLE".equals(income.getIncomeStabilityLabel()) ? 18.99 : 22.99;
        } else if ("REVIEW".equals(outcome)) {
            creditLimit = income.getAverageMonthlyIncome() == null ? 0.0
                    : round2(income.getAverageMonthlyIncome() * 0.10);
            apr = 24.99;
        } else {
            creditLimit = 0.0;
            apr = 0.0;
        }

        EligibilityDecision decision = new EligibilityDecision(
                UUID.randomUUID().toString(),
                income.getUserId(),
                outcome,
                creditLimit,
                apr,
                income.getAverageMonthlyIncome() == null ? 0.0 : income.getAverageMonthlyIncome(),
                confidence,
                income.getIncomeStabilityLabel(),
                String.join(" ", reasons),
                Instant.now()
        );

        repository.save(decision);
        log.info("Eligibility decision for userId={}: {} (limit=${}, apr={}%, reasons={})",
                decision.getUserId(), decision.getDecision(), decision.getCreditLimitUsd(),
                decision.getSuggestedApr(), decision.getReasoning());
        return decision;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
