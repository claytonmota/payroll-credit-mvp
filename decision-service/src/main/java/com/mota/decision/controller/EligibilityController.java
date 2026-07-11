package com.mota.decision.controller;

import com.mota.decision.model.EligibilityDecision;
import com.mota.decision.repository.EligibilityDecisionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/eligibility")
public class EligibilityController {

    private final EligibilityDecisionRepository repository;

    public EligibilityController(EligibilityDecisionRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns the most recent eligibility decision for a user, or all
     * decisions if ?history=true is provided (immutable audit trail).
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getForUser(@PathVariable String userId) {
        List<EligibilityDecision> all = repository.findByUserIdOrderByDecidedAtDesc(userId);
        if (all.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "not_found",
                    "message", "No eligibility decision yet for userId=" + userId
            ));
        }
        return ResponseEntity.ok(all.get(0));
    }

    @GetMapping("/{userId}/history")
    public ResponseEntity<List<EligibilityDecision>> getHistory(@PathVariable String userId) {
        return ResponseEntity.ok(repository.findByUserIdOrderByDecidedAtDesc(userId));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("service", "decision-service", "status", "UP"));
    }
}
