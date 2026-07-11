package com.mota.ingestion.controller;

import com.mota.ingestion.kafka.PayrollEventProducer;
import com.mota.ingestion.model.PayrollEvent;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Simulates the webhook/connector-adapter entry point of the Ingestion
 * Layer. In production this would be fronted by the API Gateway
 * (Kong/Apigee) and backed by real connector adapters per payroll
 * provider; for this MVP it exposes a single normalized endpoint that
 * accepts an already-normalized payroll event.
 */
@RestController
@RequestMapping("/v1/payroll")
public class PayrollEventController {

    private final PayrollEventProducer producer;

    public PayrollEventController(PayrollEventProducer producer) {
        this.producer = producer;
    }

    @PostMapping("/events")
    public ResponseEntity<Map<String, Object>> receiveEvent(@Valid @RequestBody PayrollEvent event) {
        producer.publish(event);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "status", "accepted",
                "userId", event.getUserId(),
                "message", "Payroll event queued for income verification"
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("service", "ingestion-service", "status", "UP"));
    }
}
