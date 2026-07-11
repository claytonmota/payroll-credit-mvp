package com.mota.decision.kafka;

import com.mota.decision.model.IncomeVerifiedEvent;
import com.mota.decision.service.EligibilityRulesEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class IncomeVerifiedConsumer {

    private static final Logger log = LoggerFactory.getLogger(IncomeVerifiedConsumer.class);

    private final EligibilityRulesEngine rulesEngine;

    public IncomeVerifiedConsumer(EligibilityRulesEngine rulesEngine) {
        this.rulesEngine = rulesEngine;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.income-verified:income.verified}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "incomeVerifiedListenerFactory"
    )
    public void onIncomeVerified(IncomeVerifiedEvent event) {
        log.info("Received income.verified for userId={}", event.getUserId());
        rulesEngine.evaluate(event);
    }
}
