package com.mota.incomeverification.model;

import java.io.Serializable;
import java.time.Instant;

/**
 * Payload published to the {@code income.verified} Kafka topic,
 * consumed by the Decision Service's rules engine.
 */
public class IncomeVerifiedEvent implements Serializable {

    private String userId;
    private Double averageMonthlyIncome;
    private Double incomeConfidenceScore;
    private String incomeStabilityLabel;
    private Integer payEventsConsidered;
    private Instant verifiedAt;

    public IncomeVerifiedEvent() {
    }

    public IncomeVerifiedEvent(String userId, Double averageMonthlyIncome, Double incomeConfidenceScore,
                                String incomeStabilityLabel, Integer payEventsConsidered, Instant verifiedAt) {
        this.userId = userId;
        this.averageMonthlyIncome = averageMonthlyIncome;
        this.incomeConfidenceScore = incomeConfidenceScore;
        this.incomeStabilityLabel = incomeStabilityLabel;
        this.payEventsConsidered = payEventsConsidered;
        this.verifiedAt = verifiedAt;
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

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}
