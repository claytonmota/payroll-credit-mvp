package com.mota.incomeverification.repository;

import com.mota.incomeverification.model.PayrollEventRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayrollEventRecordRepository extends JpaRepository<PayrollEventRecord, Long> {

    List<PayrollEventRecord> findByUserIdOrderByPayPeriodEndDesc(String userId);
}
