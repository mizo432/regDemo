package com.github.mizo432.reg.sales.repository;

import com.github.mizo432.reg.sales.domain.Transaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.transactionId = :transactionId")
    Optional<Transaction> findByTransactionIdForUpdate(String transactionId);

    List<Transaction> findByStatus(Transaction.Status status);

    List<Transaction> findAllByOrderByDateTimeDesc();

    Page<Transaction> findAllByOrderByDateTimeDesc(Pageable pageable);

    Page<Transaction> findByStatus(Transaction.Status status, Pageable pageable);
}
