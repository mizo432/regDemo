package com.github.mizo432.reg.sync.cqrs.handler;

import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.sync.cqrs.command.*;
import com.github.mizo432.reg.sync.domain.SyncInbox;
import com.github.mizo432.reg.sync.domain.SyncOutbox;
import com.github.mizo432.reg.sync.repository.SyncInboxRepository;
import com.github.mizo432.reg.sync.repository.SyncOutboxRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 同期関連のコマンドを処理するハンドラー
 */
@Component
@Transactional
public class SyncCommandHandler {

    private final SyncOutboxRepository syncOutboxRepository;
    private final SyncInboxRepository syncInboxRepository;

    public SyncCommandHandler(SyncOutboxRepository syncOutboxRepository,
                              SyncInboxRepository syncInboxRepository) {
        this.syncOutboxRepository = syncOutboxRepository;
        this.syncInboxRepository = syncInboxRepository;
    }

    public Void execute(MarkAsSentCommand command) {
        if (command.transactionId() == null || !StringUtils.hasText(command.transactionId())) {
            throw new ValidationException("transactionId", "取引IDは空であってはいけません");
        }
        syncOutboxRepository.findByTransactionId(command.transactionId()).ifPresent(outbox -> {
            outbox.setStatus(SyncOutbox.Status.SENT);
            syncOutboxRepository.save(outbox);
        });
        return null;
    }

    public Void execute(MarkAsFailedCommand command) {
        if (command.transactionId() == null || !StringUtils.hasText(command.transactionId())) {
            throw new ValidationException("transactionId", "取引IDは空であってはいけません");
        }
        syncOutboxRepository.findByTransactionId(command.transactionId()).ifPresent(outbox -> {
            outbox.setLastError(command.error());
            outbox.setRetryCount(outbox.getRetryCount() + 1);
            if (outbox.isExhausted()) {
                outbox.setStatus(SyncOutbox.Status.FAILED);
            }
            syncOutboxRepository.save(outbox);
        });
        return null;
    }

    public SyncInbox execute(QueueInboxCommand command) {
        if (command.eventType() == null || !StringUtils.hasText(command.eventType())) {
            throw new ValidationException("eventType", "イベントタイプは空であってはいけません");
        }
        if (command.eventId() == null || !StringUtils.hasText(command.eventId())) {
            throw new ValidationException("eventId", "イベントIDは空であってはいけません");
        }
        SyncInbox inbox = new SyncInbox();
        inbox.setEventType(command.eventType());
        inbox.setEventId(command.eventId());
        inbox.setPayload(command.payload());
        return syncInboxRepository.save(inbox);
    }

    public Void execute(MarkAsAppliedCommand command) {
        if (command.inboxId() == null) {
            throw new ValidationException("inboxId", "Inbox IDはnullであってはいけません");
        }
        syncInboxRepository.findById(command.inboxId()).ifPresent(inbox -> {
            inbox.setApplied(true);
            inbox.setAppliedAt(LocalDateTime.now());
            syncInboxRepository.save(inbox);
        });
        return null;
    }
}
