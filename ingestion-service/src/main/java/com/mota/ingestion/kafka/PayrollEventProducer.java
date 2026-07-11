package com.mota.ingestion.kafka;

import com.mota.ingestion.model.PayrollEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PayrollEventProducer {

    private static final Logger log = LoggerFactory.getLogger(PayrollEventProducer.class);

    private final KafkaTemplate<String, PayrollEvent> kafkaTemplate;

    @Value("${app.kafka.topic.payroll-events:payroll.events}")
    private String topic;

    public PayrollEventProducer(KafkaTemplate<String, PayrollEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(PayrollEvent event) {
        log.info("Publishing payroll event for userId={} to topic={}", event.getUserId(), topic);
        kafkaTemplate.send(topic, event.getUserId(), event);
    }
}
