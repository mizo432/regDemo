package com.github.mizo432.reg.sync.repository;

import com.github.mizo432.reg.sync.domain.SyncInbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SyncInboxRepository extends JpaRepository<SyncInbox, Long> {

    List<SyncInbox> findByApplied(boolean applied);
}
