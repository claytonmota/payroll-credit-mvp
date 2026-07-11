package com.mota.incomeverification.repository;

import com.mota.incomeverification.model.IncomeVerificationResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncomeVerificationRepository extends JpaRepository<IncomeVerificationResult, String> {
}
