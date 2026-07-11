package com.mota.incomeverification.controller;

import com.mota.incomeverification.model.IncomeVerificationResult;
import com.mota.incomeverification.repository.IncomeVerificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1/income")
public class IncomeVerificationController {

    private final IncomeVerificationRepository repository;

    public IncomeVerificationController(IncomeVerificationRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/verification/{userId}")
    public ResponseEntity<?> getVerification(@PathVariable String userId) {
        return repository.findById(userId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of(
                        "error", "not_found",
                        "message", "No income verification result yet for userId=" + userId
                )));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("service", "income-verification-service", "status", "UP"));
    }
}
