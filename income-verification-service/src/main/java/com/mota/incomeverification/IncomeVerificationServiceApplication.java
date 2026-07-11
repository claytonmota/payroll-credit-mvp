package com.mota.incomeverification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Income Verification Service — the technical core of the Professional
 * Plan's proposed endeavor: replacing static, historical credit scores
 * with real-time, payroll-derived income validation.
 * <p>
 * Consumes normalized payroll events from the {@code payroll.events}
 * Kafka topic, computes an income confidence score from the observed
 * pay-period history, persists the result, and publishes it to the
 * {@code income.verified} topic for the Decision Service.
 */
@SpringBootApplication
public class IncomeVerificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IncomeVerificationServiceApplication.class, args);
    }
}
