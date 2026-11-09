package com.github.mizo432.reg.sync.repository;

import com.github.mizo432.reg.sync.domain.SyncOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SyncOutboxRepository extends JpaRepository<SyncOutbox, Long> {

    List<SyncOutbox> findByStatus(SyncOutbox.Status status);
    
    Optional<SyncOutbox> findByTransactionId(String transactionId);
}
