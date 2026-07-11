package com.mota.incomeverification.model;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;

/**
 * Persisted, single-source-of-truth income verification record for a
 * user — the core artifact of Methodology 1 ("single source of truth
 * for income and payment capacity") in the Professional Plan.
 */
@Entity
@Table(name = "income_verification_result")
public class IncomeVerificationResult implements Serializable {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(name = "average_monthly_income", nullable = false)
    private Double averageMonthlyIncome;

    @Column(name = "income_confidence_score", nullable = false)
    private Double incomeConfidenceScore; // 0.0 - 1.0

    @Column(name = "income_stability_label", nullable = false)
    private String incomeStabilityLabel; // STABLE, MODERATE, VOLATILE, INSUFFICIENT_DATA

    @Column(name = "pay_events_considered", nullable = false)
    private Integer payEventsConsidered;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    public IncomeVerificationResult() {
    }

    public IncomeVerificationResult(String userId, Double averageMonthlyIncome,
                                     Double incomeConfidenceScore, String incomeStabilityLabel,
                                     Integer payEventsConsidered, Instant lastUpdated) {
        this.userId = userId;
        this.averageMonthlyIncome = averageMonthlyIncome;
        this.incomeConfidenceScore = incomeConfidenceScore;
        this.incomeStabilityLabel = incomeStabilityLabel;
        this.payEventsConsidered = payEventsConsidered;
        this.lastUpdated = lastUpdated;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Double getAverageMonthlyIncome() {
        return averageMonthlyIncome;
    }

    public void setAverageMonthlyIncome(Double averageMonthlyIncome) {
        this.averageMonthlyIncome = averageMonthlyIncome;
    }

    public Double getIncomeConfidenceScore() {
        return incomeConfidenceScore;
    }

    public void setIncomeConfidenceScore(Double incomeConfidenceScore) {
        this.incomeConfidenceScore = incomeConfidenceScore;
    }

    public String getIncomeStabilityLabel() {
        return incomeStabilityLabel;
    }

    public void setIncomeStabilityLabel(String incomeStabilityLabel) {
        this.incomeStabilityLabel = incomeStabilityLabel;
    }

    public Integer getPayEventsConsidered() {
        return payEventsConsidered;
    }

    public void setPayEventsConsidered(Integer payEventsConsidered) {
        this.payEventsConsidered = payEventsConsidered;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
