package com.mota.decision.repository;

import com.mota.decision.model.EligibilityDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EligibilityDecisionRepository extends JpaRepository<EligibilityDecision, String> {

    List<EligibilityDecision> findByUserIdOrderByDecidedAtDesc(String userId);
}
