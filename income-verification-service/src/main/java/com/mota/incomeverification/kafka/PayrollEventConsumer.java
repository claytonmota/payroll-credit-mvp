package com.mota.incomeverification.kafka;

import com.mota.incomeverification.model.IncomeVerifiedEvent;
import com.mota.incomeverification.model.PayrollEvent;
import com.mota.incomeverification.service.IncomeValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PayrollEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PayrollEventConsumer.class);

    private final IncomeValidationService incomeValidationService;
    private final KafkaTemplate<String, IncomeVerifiedEvent> incomeVerifiedKafkaTemplate;

    @Value("${app.kafka.topic.income-verified:income.verified}")
    private String incomeVerifiedTopic;

    public PayrollEventConsumer(IncomeValidationService incomeValidationService,
                                 KafkaTemplate<String, IncomeVerifiedEvent> incomeVerifiedKafkaTemplate) {
        this.incomeValidationService = incomeValidationService;
        this.incomeVerifiedKafkaTemplate = incomeVerifiedKafkaTemplate;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.payroll-events:payroll.events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "payrollEventListenerFactory"
    )
    public void onPayrollEvent(PayrollEvent event) {
        log.info("Received payroll event for userId={}", event.getUserId());
        IncomeVerifiedEvent verified = incomeValidationService.processPayrollEvent(event);
        incomeVerifiedKafkaTemplate.send(incomeVerifiedTopic, verified.getUserId(), verified);
        log.info("Published income.verified event for userId={}", verified.getUserId());
    }
}
