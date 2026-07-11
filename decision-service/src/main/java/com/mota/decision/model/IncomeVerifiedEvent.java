package com.mota.decision.model;

import java.io.Serializable;
import java.time.Instant;

/**
 * Mirror of income-verification-service's IncomeVerifiedEvent contract.
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

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Double getAverageMonthlyIncome() { return averageMonthlyIncome; }
    public void setAverageMonthlyIncome(Double averageMonthlyIncome) { this.averageMonthlyIncome = averageMonthlyIncome; }

    public Double getIncomeConfidenceScore() { return incomeConfidenceScore; }
    public void setIncomeConfidenceScore(Double incomeConfidenceScore) { this.incomeConfidenceScore = incomeConfidenceScore; }

    public String getIncomeStabilityLabel() { return incomeStabilityLabel; }
    public void setIncomeStabilityLabel(String incomeStabilityLabel) { this.incomeStabilityLabel = incomeStabilityLabel; }

    public Integer getPayEventsConsidered() { return payEventsConsidered; }
    public void setPayEventsConsidered(Integer payEventsConsidered) { this.payEventsConsidered = payEventsConsidered; }

    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
}
