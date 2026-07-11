package com.mota.incomeverification.service;

import com.mota.incomeverification.model.IncomeVerificationResult;
import com.mota.incomeverification.model.IncomeVerifiedEvent;
import com.mota.incomeverification.model.PayrollEvent;
import com.mota.incomeverification.model.PayrollEventRecord;
import com.mota.incomeverification.repository.IncomeVerificationRepository;
import com.mota.incomeverification.repository.PayrollEventRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IncomeValidationServiceTest {

    private PayrollEventRecordRepository payrollEventRecordRepository;
    private IncomeVerificationRepository incomeVerificationRepository;
    private IncomeValidationService service;

    @BeforeEach
    void setUp() {
        payrollEventRecordRepository = mock(PayrollEventRecordRepository.class);
        incomeVerificationRepository = mock(IncomeVerificationRepository.class);
        service = new IncomeValidationService(payrollEventRecordRepository, incomeVerificationRepository);
    }

    @Test
    void stableMonthlyIncome_yieldsHighConfidenceAndStableLabel() {
        // Simulate 6 consecutive monthly pay periods of consistent net pay.
        List<PayrollEventRecord> history = buildHistory(6, "MONTHLY", 5000.0, 0.0);
        when(payrollEventRecordRepository.findByUserIdOrderByPayPeriodEndDesc("user-1001"))
                .thenReturn(history);

        PayrollEvent latest = newEvent("user-1001", "MONTHLY", 5000.0);
        IncomeVerifiedEvent result = service.processPayrollEvent(latest);

        assertEquals("STABLE", result.getIncomeStabilityLabel());
        assertTrue(result.getIncomeConfidenceScore() >= 0.85,
                "Expected high confidence for perfectly stable income, got " + result.getIncomeConfidenceScore());
        assertEquals(5000.0, result.getAverageMonthlyIncome(), 0.01);

        ArgumentCaptor<IncomeVerificationResult> captor = ArgumentCaptor.forClass(IncomeVerificationResult.class);
        verify(incomeVerificationRepository).save(captor.capture());
        assertEquals("user-1001", captor.getValue().getUserId());
    }

    @Test
    void highlyVariableIncome_yieldsVolatileLabel() {
        // Alternate between very high and very low net pay to force high variance.
        List<PayrollEventRecord> history = new ArrayList<>();
        double[] values = {8000, 500, 7500, 400, 9000, 300};
        for (double v : values) {
            history.add(buildRecord("user-2002", "MONTHLY", v));
        }
        when(payrollEventRecordRepository.findByUserIdOrderByPayPeriodEndDesc("user-2002"))
                .thenReturn(history);

        PayrollEvent latest = newEvent("user-2002", "MONTHLY", 300.0);
        IncomeVerifiedEvent result = service.processPayrollEvent(latest);

        assertEquals("VOLATILE", result.getIncomeStabilityLabel());
        assertTrue(result.getIncomeConfidenceScore() < 0.6,
                "Expected low confidence for highly volatile income, got " + result.getIncomeConfidenceScore());
    }

    @Test
    void singlePayEvent_isInsufficientData() {
        when(payrollEventRecordRepository.findByUserIdOrderByPayPeriodEndDesc("user-3003"))
                .thenReturn(List.of(buildRecord("user-3003", "MONTHLY", 4000.0)));

        PayrollEvent latest = newEvent("user-3003", "MONTHLY", 4000.0);
        IncomeVerifiedEvent result = service.processPayrollEvent(latest);

        assertEquals("INSUFFICIENT_DATA", result.getIncomeStabilityLabel());
        assertEquals(1, result.getPayEventsConsidered());
    }

    @Test
    void weeklyPayFrequency_isNormalizedToMonthlyEquivalent() {
        // $1,000/week * 4.33 ~= $4,330/month
        List<PayrollEventRecord> history = buildHistory(4, "WEEKLY", 1000.0, 0.0);
        when(payrollEventRecordRepository.findByUserIdOrderByPayPeriodEndDesc("user-4004"))
                .thenReturn(history);

        PayrollEvent latest = newEvent("user-4004", "WEEKLY", 1000.0);
        IncomeVerifiedEvent result = service.processPayrollEvent(latest);

        assertEquals(4330.0, result.getAverageMonthlyIncome(), 1.0);
    }

    // ---- helpers ----

    private PayrollEvent newEvent(String userId, String frequency, double netPay) {
        PayrollEvent event = new PayrollEvent();
        event.setUserId(userId);
        event.setEmployerName("Test Employer Inc.");
        event.setPayPeriodStart(LocalDate.now().minusDays(14));
        event.setPayPeriodEnd(LocalDate.now());
        event.setGrossPay(netPay * 1.25);
        event.setNetPay(netPay);
        event.setPayFrequency(frequency);
        event.setSourceProvider("TestProvider");
        return event;
    }

    private List<PayrollEventRecord> buildHistory(int count, String frequency, double baseNetPay, double noise) {
        List<PayrollEventRecord> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(buildRecord("user-1001", frequency, baseNetPay + noise));
        }
        return list;
    }

    private PayrollEventRecord buildRecord(String userId, String frequency, double netPay) {
        PayrollEventRecord r = new PayrollEventRecord();
        r.setUserId(userId);
        r.setEmployerName("Test Employer Inc.");
        r.setPayPeriodStart(LocalDate.now().minusDays(30));
        r.setPayPeriodEnd(LocalDate.now());
        r.setGrossPay(netPay * 1.25);
        r.setNetPay(netPay);
        r.setPayFrequency(frequency);
        r.setSourceProvider("TestProvider");
        return r;
    }
}
