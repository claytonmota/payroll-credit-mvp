package com.mota.ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ingestion Layer entry point.
 * <p>
 * Exposes REST endpoints that simulate payroll-provider / employer
 * webhooks (ADP, Workday, Paychex, Gusto, Rippling, etc. in production)
 * and publishes normalized payroll events onto the {@code payroll.events}
 * Kafka topic for downstream consumption by the Income Verification
 * Service, per the Professional Plan's Methodology 1 (Integrated Data
 * Architecture & Interoperability).
 */
@SpringBootApplication
public class IngestionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngestionServiceApplication.class, args);
    }
}
