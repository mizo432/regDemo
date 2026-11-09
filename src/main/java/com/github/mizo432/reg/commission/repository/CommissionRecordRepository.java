package com.github.mizo432.reg.commission.repository;

import com.github.mizo432.reg.commission.domain.CommissionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommissionRecordRepository extends JpaRepository<CommissionRecord, Long> {

    List<CommissionRecord> findByTransactionId(Long transactionId);
}
