package com.mota.decision;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Decision Service — implements the "Rules Engine" component of the
 * Risk Orchestration Engine and the Decision Service block from the
 * architecture diagram.
 * <p>
 * Consumes income.verified events, applies a transparent, auditable
 * rules-based eligibility model (deliberately not a black-box ML model,
 * matching the Professional Plan's governance & auditability goals),
 * and persists the resulting decision as an immutable audit record.
 */
@SpringBootApplication
public class DecisionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DecisionServiceApplication.class, args);
    }
}
