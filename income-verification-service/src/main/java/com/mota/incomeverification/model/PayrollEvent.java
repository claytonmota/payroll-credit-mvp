package com.mota.incomeverification.model;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Mirror of ingestion-service's PayrollEvent contract. Kept as a
 * separate DTO (rather than a shared library) to preserve independent
 * service deployability, per the domain-driven microservices approach
 * in the architecture diagram. A future iteration may extract this into
 * a shared schema module registered with the Schema Registry
 * (Avro/JSON), as shown in the "Event Streaming & Integration" layer.
 */
public class PayrollEvent implements Serializable {

    private String userId;
    private String employerName;
    private LocalDate payPeriodStart;
    private LocalDate payPeriodEnd;
    private Double grossPay;
    private Double netPay;
    private String payFrequency;
    private String sourceProvider;

    public PayrollEvent() {
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
}
