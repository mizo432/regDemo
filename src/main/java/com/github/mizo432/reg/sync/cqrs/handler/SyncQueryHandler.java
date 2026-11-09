package com.github.mizo432.reg.sync.cqrs.handler;

import com.github.mizo432.reg.sync.cqrs.query.*;
import com.github.mizo432.reg.sync.domain.SyncInbox;
import com.github.mizo432.reg.sync.domain.SyncOutbox;
import com.github.mizo432.reg.sync.repository.SyncInboxRepository;
import com.github.mizo432.reg.sync.repository.SyncOutboxRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 同期関連のクエリを処理するハンドラー
 */
@Component
@Transactional(readOnly = true)
public class SyncQueryHandler {

    private final SyncOutboxRepository syncOutboxRepository;
    private final SyncInboxRepository syncInboxRepository;

    public SyncQueryHandler(SyncOutboxRepository syncOutboxRepository,
                            SyncInboxRepository syncInboxRepository) {
        this.syncOutboxRepository = syncOutboxRepository;
        this.syncInboxRepository = syncInboxRepository;
    }

    public List<SyncOutbox> handle(GetPendingOutboxQuery query) {
        return syncOutboxRepository.findByStatus(SyncOutbox.Status.PENDING);
    }

    public List<SyncOutbox> handle(GetSentOutboxQuery query) {
        return syncOutboxRepository.findByStatus(SyncOutbox.Status.SENT);
    }

    public List<SyncOutbox> handle(GetFailedOutboxQuery query) {
        return syncOutboxRepository.findByStatus(SyncOutbox.Status.FAILED);
    }

    public List<SyncInbox> handle(GetUnappliedInboxQuery query) {
        return syncInboxRepository.findByApplied(false);
    }

    public List<SyncOutbox> handle(GetAllOutboxQuery query) {
        return syncOutboxRepository.findAll();
    }

    public List<SyncInbox> handle(GetAllInboxQuery query) {
        return syncInboxRepository.findAll();
    }
}
