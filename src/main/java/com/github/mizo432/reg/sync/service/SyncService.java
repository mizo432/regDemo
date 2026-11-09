package com.github.mizo432.reg.sync.service;

import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.sales.domain.Transaction;
import com.github.mizo432.reg.sales.domain.TransactionItem;
import com.github.mizo432.reg.sync.cqrs.command.MarkAsAppliedCommand;
import com.github.mizo432.reg.sync.cqrs.command.MarkAsFailedCommand;
import com.github.mizo432.reg.sync.cqrs.command.MarkAsSentCommand;
import com.github.mizo432.reg.sync.cqrs.command.QueueInboxCommand;
import com.github.mizo432.reg.sync.cqrs.handler.SyncCommandHandler;
import com.github.mizo432.reg.sync.cqrs.handler.SyncQueryHandler;
import com.github.mizo432.reg.sync.cqrs.query.*;
import com.github.mizo432.reg.sync.domain.SyncInbox;
import com.github.mizo432.reg.sync.domain.SyncOutbox;
import com.github.mizo432.reg.sync.repository.SyncInboxRepository;
import com.github.mizo432.reg.sync.repository.SyncOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 同期サービスのファサード。
 * CQRSパターンを採用し、コマンド操作はSyncCommandHandler、クエリ操作はSyncQueryHandlerに委譲します。
 * 既存のAPIを維持するため、このファサードクラスを介してアクセスします。
 */
@Service
public class SyncService {

    private final SyncCommandHandler commandHandler;
    private final SyncQueryHandler queryHandler;
    private final SyncOutboxRepository syncOutboxRepository;
    private final SyncInboxRepository syncInboxRepository;

    public SyncService(SyncCommandHandler commandHandler,
                       SyncQueryHandler queryHandler,
                       SyncOutboxRepository syncOutboxRepository,
                       SyncInboxRepository syncInboxRepository) {
        this.commandHandler = commandHandler;
        this.queryHandler = queryHandler;
        this.syncOutboxRepository = syncOutboxRepository;
        this.syncInboxRepository = syncInboxRepository;
    }

    public List<SyncOutbox> findPendingOutbox() {
        return queryHandler.handle(new GetPendingOutboxQuery());
    }

    public List<SyncOutbox> findSentOutbox() {
        return queryHandler.handle(new GetSentOutboxQuery());
    }

    public List<SyncOutbox> findFailedOutbox() {
        return queryHandler.handle(new GetFailedOutboxQuery());
    }

    public List<SyncInbox> findUnappliedInbox() {
        return queryHandler.handle(new GetUnappliedInboxQuery());
    }

    public List<SyncOutbox> findAllOutbox() {
        return queryHandler.handle(new GetAllOutboxQuery());
    }

    public List<SyncInbox> findAllInbox() {
        return queryHandler.handle(new GetAllInboxQuery());
    }

    @Transactional
    public void queueTransaction(Transaction transaction) {
        // 入力検証
        if (transaction == null) {
            throw new ValidationException("transaction", "トランザクションはnullであってはいけません");
        }
        if (transaction.getTransactionId() == null || !StringUtils.hasText(transaction.getTransactionId())) {
            throw new ValidationException("transactionId", "取引IDは空であってはいけません");
        }
        SyncOutbox outbox = new SyncOutbox();
        outbox.setTransactionId(transaction.getTransactionId());
        List<TransactionItemPayload> items = transaction.getItems().stream().map(item ->
                new TransactionItemPayload(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getTotalAmount(),
                        item.getTaxAmount()
                )
        ).collect(Collectors.toList());
        TransactionPayload payload = new TransactionPayload(
                transaction.getTransactionId(),
                transaction.getSubtotal() != null ? transaction.getSubtotal() : BigDecimal.ZERO,
                transaction.getTaxTotal() != null ? transaction.getTaxTotal() : BigDecimal.ZERO,
                transaction.getGrandTotal() != null ? transaction.getGrandTotal() : BigDecimal.ZERO,
                transaction.getPaymentMethod() != null ? transaction.getPaymentMethod().name() : "",
                transaction.getStatus().name(),
                transaction.getDateTime(),
                items
        );
        try {
            outbox.setPayload(JsonMapper.shared().writeValueAsString(payload));
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to serialize transaction payload", e);
        }
        outbox.setStatus(SyncOutbox.Status.PENDING);
        syncOutboxRepository.save(outbox);
    }

    private record TransactionPayload(
            String transactionId,
            BigDecimal subtotal,
            BigDecimal taxTotal,
            BigDecimal grandTotal,
            String paymentMethod,
            String status,
            LocalDateTime dateTime,
            List<TransactionItemPayload> items
    ) {}

    private record TransactionItemPayload(
            Long productId,
            String productName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal totalAmount,
            BigDecimal taxAmount
    ) {}

    public void markAsSent(String transactionId) {
        if (transactionId == null || !StringUtils.hasText(transactionId)) {
            throw new ValidationException("transactionId", "取引IDは空であってはいけません");
        }
        commandHandler.execute(new MarkAsSentCommand(transactionId));
    }

    public void markAsFailed(String transactionId, String error) {
        if (transactionId == null || !StringUtils.hasText(transactionId)) {
            throw new ValidationException("transactionId", "取引IDは空であってはいけません");
        }
        commandHandler.execute(new MarkAsFailedCommand(transactionId, error));
    }

    @Transactional
    public void markSentAsFailed(String transactionId, String error) {
        syncOutboxRepository.findByTransactionId(transactionId).ifPresent(outbox -> {
            if (outbox.getStatus() == SyncOutbox.Status.SENT) {
                outbox.setLastError(error);
                outbox.setRetryCount(outbox.getRetryCount() + 1);
                if (outbox.isExhausted()) {
                    outbox.setStatus(SyncOutbox.Status.FAILED);
                } else {
                    outbox.setStatus(SyncOutbox.Status.PENDING);
                }
                syncOutboxRepository.save(outbox);
            }
        });
    }

    public SyncInbox queueInbox(String eventType, String eventId, String payload) {
        if (eventType == null || !StringUtils.hasText(eventType)) {
            throw new ValidationException("eventType", "イベントタイプは空であってはいけません");
        }
        if (eventId == null || !StringUtils.hasText(eventId)) {
            throw new ValidationException("eventId", "イベントIDは空であってはいけません");
        }
        return commandHandler.execute(new QueueInboxCommand(eventType, eventId, payload));
    }

    public void markAsApplied(Long inboxId) {
        if (inboxId == null) {
            throw new ValidationException("inboxId", "Inbox IDはnullであってはいけません");
        }
        commandHandler.execute(new MarkAsAppliedCommand(inboxId));
    }
}
