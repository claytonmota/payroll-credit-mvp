package com.mota.decision.service;

import com.mota.decision.model.EligibilityDecision;
import com.mota.decision.model.IncomeVerifiedEvent;
import com.mota.decision.repository.EligibilityDecisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EligibilityRulesEngineTest {

    private EligibilityDecisionRepository repository;
    private EligibilityRulesEngine engine;

    @BeforeEach
    void setUp() {
        repository = mock(EligibilityDecisionRepository.class);
        when(repository.save(any(EligibilityDecision.class))).thenAnswer(inv -> inv.getArgument(0));
        engine = new EligibilityRulesEngine(repository);
    }

    @Test
    void stableHighIncomeUser_isApproved() {
        IncomeVerifiedEvent evt = income("user-1", 5000.0, 0.95, "STABLE", 6);
        EligibilityDecision d = engine.evaluate(evt);
        assertEquals("APPROVED", d.getDecision());
        assertTrue(d.getCreditLimitUsd() > 0);
        assertTrue(d.getSuggestedApr() > 0);
        assertTrue(d.getReasoning().toLowerCase().contains("stable"));
    }

    @Test
    void insufficientPayHistory_goesToReview() {
        IncomeVerifiedEvent evt = income("user-2", 5000.0, 0.9, "STABLE", 1);
        EligibilityDecision d = engine.evaluate(evt);
        assertEquals("REVIEW", d.getDecision());
        assertTrue(d.getReasoning().toLowerCase().contains("insufficient"));
    }

    @Test
    void incomeBelowFloor_isDenied() {
        IncomeVerifiedEvent evt = income("user-3", 500.0, 0.95, "STABLE", 6);
        EligibilityDecision d = engine.evaluate(evt);
        assertEquals("DENIED", d.getDecision());
        assertEquals(0.0, d.getCreditLimitUsd());
    }

    @Test
    void moderateConfidence_goesToReview() {
        IncomeVerifiedEvent evt = income("user-4", 4000.0, 0.65, "MODERATE", 6);
        EligibilityDecision d = engine.evaluate(evt);
        assertEquals("REVIEW", d.getDecision());
    }

    @Test
    void veryLowConfidence_isDenied() {
        IncomeVerifiedEvent evt = income("user-5", 4000.0, 0.3, "VOLATILE", 6);
        EligibilityDecision d = engine.evaluate(evt);
        assertEquals("DENIED", d.getDecision());
    }

    private IncomeVerifiedEvent income(String userId, double avgIncome, double confidence,
                                        String label, int events) {
        IncomeVerifiedEvent e = new IncomeVerifiedEvent();
        e.setUserId(userId);
        e.setAverageMonthlyIncome(avgIncome);
        e.setIncomeConfidenceScore(confidence);
        e.setIncomeStabilityLabel(label);
        e.setPayEventsConsidered(events);
        e.setVerifiedAt(Instant.now());
        return e;
    }
}
