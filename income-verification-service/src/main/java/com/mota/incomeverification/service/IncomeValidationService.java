package com.mota.incomeverification.service;

import com.mota.incomeverification.model.IncomeVerificationResult;
import com.mota.incomeverification.model.IncomeVerifiedEvent;
import com.mota.incomeverification.model.PayrollEvent;
import com.mota.incomeverification.model.PayrollEventRecord;
import com.mota.incomeverification.repository.IncomeVerificationRepository;
import com.mota.incomeverification.repository.PayrollEventRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Computes a real-time "income confidence score" from a rolling window
 * of a user's payroll events — the technical mechanism behind the
 * Professional Plan's central claim: replacing reliance on static,
 * historical credit scores with a real-time, payroll-derived view of
 * income and payment capacity.
 * <p>
 * Method (deliberately transparent / explainable, matching the
 * "Rules Engine" component of the Risk Orchestration Engine rather than
 * an opaque ML model):
 * <ol>
 *   <li>Normalize each pay event's net pay to a monthly-equivalent
 *       amount based on its pay frequency.</li>
 *   <li>Compute the mean and coefficient of variation (CV = stddev/mean)
 *       of monthly-equivalent income over the last N pay events.</li>
 *   <li>Confidence score = 1 - CV, clamped to [0, 1]. Lower variability
 *       across pay periods yields a higher confidence score.</li>
 *   <li>Stability label is derived from thresholds on that score.</li>
 * </ol>
 */
@Service
public class IncomeValidationService {

    private static final Logger log = LoggerFactory.getLogger(IncomeValidationService.class);

    private static final int MAX_EVENTS_CONSIDERED = 12;

    private final PayrollEventRecordRepository payrollEventRecordRepository;
    private final IncomeVerificationRepository incomeVerificationRepository;

    public IncomeValidationService(PayrollEventRecordRepository payrollEventRecordRepository,
                                    IncomeVerificationRepository incomeVerificationRepository) {
        this.payrollEventRecordRepository = payrollEventRecordRepository;
        this.incomeVerificationRepository = incomeVerificationRepository;
    }

    @Transactional
    public IncomeVerifiedEvent processPayrollEvent(PayrollEvent event) {
        persistRawEvent(event);

        List<PayrollEventRecord> history = payrollEventRecordRepository
                .findByUserIdOrderByPayPeriodEndDesc(event.getUserId());

        int windowSize = Math.min(history.size(), MAX_EVENTS_CONSIDERED);
        List<PayrollEventRecord> window = history.subList(0, windowSize);

        double[] monthlyEquivalents = window.stream()
                .mapToDouble(r -> toMonthlyEquivalent(r.getNetPay(), r.getPayFrequency()))
                .toArray();

        Result result = computeConfidence(monthlyEquivalents);

        IncomeVerificationResult persisted = new IncomeVerificationResult(
                event.getUserId(),
                round2(result.mean),
                round2(result.confidenceScore),
                result.label,
                windowSize,
                Instant.now()
        );
        incomeVerificationRepository.save(persisted);

        log.info("Computed income verification for userId={}: avgMonthlyIncome={}, confidence={}, label={}, events={}",
                event.getUserId(), persisted.getAverageMonthlyIncome(), persisted.getIncomeConfidenceScore(),
                persisted.getIncomeStabilityLabel(), windowSize);

        return new IncomeVerifiedEvent(
                event.getUserId(),
                persisted.getAverageMonthlyIncome(),
                persisted.getIncomeConfidenceScore(),
                persisted.getIncomeStabilityLabel(),
                windowSize,
                persisted.getLastUpdated()
        );
    }

    private void persistRawEvent(PayrollEvent event) {
        PayrollEventRecord record = new PayrollEventRecord();
        record.setUserId(event.getUserId());
        record.setEmployerName(event.getEmployerName());
        record.setPayPeriodStart(event.getPayPeriodStart());
        record.setPayPeriodEnd(event.getPayPeriodEnd());
        record.setGrossPay(event.getGrossPay());
        record.setNetPay(event.getNetPay());
        record.setPayFrequency(event.getPayFrequency());
        record.setSourceProvider(event.getSourceProvider());
        record.setReceivedAt(Instant.now());
        payrollEventRecordRepository.save(record);
    }

    private double toMonthlyEquivalent(double netPay, String payFrequency) {
        if (payFrequency == null) {
            return netPay;
        }
        return switch (payFrequency.toUpperCase()) {
            case "WEEKLY" -> netPay * 4.33;
            case "BIWEEKLY" -> netPay * 2.166;
            case "SEMIMONTHLY" -> netPay * 2.0;
            case "MONTHLY" -> netPay;
            default -> netPay;
        };
    }

    private Result computeConfidence(double[] values) {
        if (values.length == 0) {
            return new Result(0.0, 0.0, "INSUFFICIENT_DATA");
        }
        if (values.length == 1) {
            // A single data point cannot establish stability; assign a
            // conservative baseline confidence rather than a false 1.0.
            return new Result(values[0], 0.4, "INSUFFICIENT_DATA");
        }

        double mean = 0.0;
        for (double v : values) mean += v;
        mean /= values.length;

        double variance = 0.0;
        for (double v : values) variance += Math.pow(v - mean, 2);
        variance /= values.length;
        double stdDev = Math.sqrt(variance);

        double cv = mean == 0 ? 1.0 : stdDev / mean;
        double confidence = clamp(1.0 - cv, 0.0, 1.0);

        String label;
        if (confidence >= 0.85) {
            label = "STABLE";
        } else if (confidence >= 0.6) {
            label = "MODERATE";
        } else {
            label = "VOLATILE";
        }

        return new Result(mean, confidence, label);
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private record Result(double mean, double confidenceScore, String label) {
    }
}
