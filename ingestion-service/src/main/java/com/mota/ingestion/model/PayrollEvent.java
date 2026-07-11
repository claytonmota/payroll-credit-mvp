package com.mota.ingestion.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Normalized payroll pay-period event, as received (in production) from a
 * payroll provider connector adapter (ADP, Workday, Paychex, Gusto,
 * Rippling) via the Ingestion Layer's API Gateway / connector adapters.
 * <p>
 * This is the canonical event contract published to the
 * {@code payroll.events} Kafka topic and consumed by the Income
 * Verification Service.
 */
public class PayrollEvent implements Serializable {

    @NotBlank
    private String userId;

    @NotBlank
    private String employerName;

    @NotNull
    private LocalDate payPeriodStart;

    @NotNull
    private LocalDate payPeriodEnd;

    @NotNull
    @Positive
    private Double grossPay;

    @NotNull
    @Positive
    private Double netPay;

    @NotBlank
    private String payFrequency; // WEEKLY, BIWEEKLY, SEMIMONTHLY, MONTHLY

    private String sourceProvider; // e.g. "ADP", "Gusto" (simulated)

    public PayrollEvent() {
    }

    public PayrollEvent(String userId, String employerName, LocalDate payPeriodStart,
                         LocalDate payPeriodEnd, Double grossPay, Double netPay,
                         String payFrequency, String sourceProvider) {
        this.userId = userId;
        this.employerName = employerName;
        this.payPeriodStart = payPeriodStart;
        this.payPeriodEnd = payPeriodEnd;
        this.grossPay = grossPay;
        this.netPay = netPay;
        this.payFrequency = payFrequency;
        this.sourceProvider = sourceProvider;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmployerName() {
        return employerName;
    }

    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }

    public LocalDate getPayPeriodStart() {
        return payPeriodStart;
    }

    public void setPayPeriodStart(LocalDate payPeriodStart) {
        this.payPeriodStart = payPeriodStart;
    }

    public LocalDate getPayPeriodEnd() {
        return payPeriodEnd;
    }

    public void setPayPeriodEnd(LocalDate payPeriodEnd) {
        this.payPeriodEnd = payPeriodEnd;
    }

    public Double getGrossPay() {
        return grossPay;
    }

    public void setGrossPay(Double grossPay) {
        this.grossPay = grossPay;
    }

    public Double getNetPay() {
        return netPay;
    }

    public void setNetPay(Double netPay) {
        this.netPay = netPay;
    }

    public String getPayFrequency() {
        return payFrequency;
    }

    public void setPayFrequency(String payFrequency) {
        this.payFrequency = payFrequency;
    }

    public String getSourceProvider() {
        return sourceProvider;
    }

    public void setSourceProvider(String sourceProvider) {
        this.sourceProvider = sourceProvider;
    }

    @Override
    public String toString() {
        return "PayrollEvent{" +
                "userId='" + userId + '\'' +
                ", employerName='" + employerName + '\'' +
                ", payPeriodStart=" + payPeriodStart +
                ", payPeriodEnd=" + payPeriodEnd +
                ", grossPay=" + grossPay +
                ", netPay=" + netPay +
                ", payFrequency='" + payFrequency + '\'' +
                ", sourceProvider='" + sourceProvider + '\'' +
                '}';
    }
}
