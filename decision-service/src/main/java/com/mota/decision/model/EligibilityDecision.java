package com.mota.decision.model;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;

/**
 * Immutable, append-only record of every eligibility decision made by
 * the platform — one row per decision, keyed by decisionId. This table
 * is the concrete implementation of the Professional Plan's
 * Methodology 3 ("Advanced Data Governance & Financial Compliance"):
 * every credit decision is backed by a verifiable, auditable trail that
 * can be reconstructed after the fact for regulatory review
 * (Basel II, SOC 2, CFPB reporting).
 */
@Entity
@Table(name = "eligibility_decision")
public class EligibilityDecision implements Serializable {

    @Id
    @Column(name = "decision_id", length = 64)
    private String decisionId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "decision", nullable = false, length = 32)
    private String decision; // APPROVED, REVIEW, DENIED

    @Column(name = "credit_limit_usd", nullable = false)
    private Double creditLimitUsd;

    @Column(name = "suggested_apr", nullable = false)
    private Double suggestedApr;

    @Column(name = "average_monthly_income", nullable = false)
    private Double averageMonthlyIncome;

    @Column(name = "income_confidence_score", nullable = false)
    private Double incomeConfidenceScore;

    @Column(name = "income_stability_label", nullable = false)
    private String incomeStabilityLabel;

    @Column(name = "reasoning", nullable = false, length = 2000)
    private String reasoning;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    public EligibilityDecision() {
    }

    public EligibilityDecision(String decisionId, String userId, String decision,
                                Double creditLimitUsd, Double suggestedApr,
                                Double averageMonthlyIncome, Double incomeConfidenceScore,
                                String incomeStabilityLabel, String reasoning, Instant decidedAt) {
        this.decisionId = decisionId;
        this.userId = userId;
        this.decision = decision;
        this.creditLimitUsd = creditLimitUsd;
        this.suggestedApr = suggestedApr;
        this.averageMonthlyIncome = averageMonthlyIncome;
        this.incomeConfidenceScore = incomeConfidenceScore;
        this.incomeStabilityLabel = incomeStabilityLabel;
        this.reasoning = reasoning;
        this.decidedAt = decidedAt;
    }

    public String getDecisionId() { return decisionId; }
    public void setDecisionId(String decisionId) { this.decisionId = decisionId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public Double getCreditLimitUsd() { return creditLimitUsd; }
    public void setCreditLimitUsd(Double creditLimitUsd) { this.creditLimitUsd = creditLimitUsd; }

    public Double getSuggestedApr() { return suggestedApr; }
    public void setSuggestedApr(Double suggestedApr) { this.suggestedApr = suggestedApr; }

    public Double getAverageMonthlyIncome() { return averageMonthlyIncome; }
    public void setAverageMonthlyIncome(Double averageMonthlyIncome) { this.averageMonthlyIncome = averageMonthlyIncome; }

    public Double getIncomeConfidenceScore() { return incomeConfidenceScore; }
    public void setIncomeConfidenceScore(Double incomeConfidenceScore) { this.incomeConfidenceScore = incomeConfidenceScore; }

    public String getIncomeStabilityLabel() { return incomeStabilityLabel; }
    public void setIncomeStabilityLabel(String incomeStabilityLabel) { this.incomeStabilityLabel = incomeStabilityLabel; }

    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }

    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
}
