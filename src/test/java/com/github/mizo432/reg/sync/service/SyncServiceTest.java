package com.github.mizo432.reg.sync.service;

import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.sync.domain.SyncOutbox;
import com.github.mizo432.reg.sync.repository.SyncOutboxRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class SyncServiceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SyncService syncService;

    @Autowired
    private SyncOutboxRepository syncOutboxRepository;

    private String transactionId;

    @BeforeEach
    void setUp() {
        transactionId = "TXN-TEST-001";
    }

    @Test
    void findSentOutbox_SENT状態のエントリを返すこと() {
        // Given - SENT状態のOutboxエントリを作成
        SyncOutbox outbox = new SyncOutbox();
        outbox.setTransactionId(transactionId);
        outbox.setPayload("{\"test\": true}");
        outbox.setStatus(SyncOutbox.Status.SENT);
        syncOutboxRepository.save(outbox);
        entityManager.flush();

        // When
        List<SyncOutbox> result = syncService.findSentOutbox();

        // Then
        assertEquals(1, result.size(), "SENT状態のエントリが返されるべき");
        assertEquals(SyncOutbox.Status.SENT, result.get(0).getStatus());
    }

    @Test
    void markSentAsFailed_SENT状態をPENDING状態に戻すこと() {
        // Given - SENT状態のOutboxエントリを作成
        SyncOutbox outbox = new SyncOutbox();
        outbox.setTransactionId(transactionId);
        outbox.setPayload("{\"test\": true}");
        outbox.setStatus(SyncOutbox.Status.SENT);
        outbox.setRetryCount(0);
        syncOutboxRepository.save(outbox);
        entityManager.flush();

        // When - 受信側で失敗したと通知
        syncService.markSentAsFailed(transactionId, "Connection timeout");

        // Then - PENDING状態に戻り、retryCountが増加
        entityManager.flush();
        entityManager.clear();
        SyncOutbox updated = syncOutboxRepository.findByTransactionId(transactionId).orElseThrow();
        assertEquals(SyncOutbox.Status.PENDING, updated.getStatus(),
                "SENT状態はPENDING状態に戻されるべき");
        assertEquals(1, updated.getRetryCount(), "retryCountは1増加するべき");
        assertEquals("Connection timeout", updated.getLastError());
    }

    @Test
    void markSentAsFailed_リトライ上限超過時はFAILED状態にすること() {
        // Given - SENT状態のOutboxエントリを作成、retryCount = 2（MAX_RETRY=3の直前）
        SyncOutbox outbox = new SyncOutbox();
        outbox.setTransactionId(transactionId);
        outbox.setPayload("{\"test\": true}");
        outbox.setStatus(SyncOutbox.Status.SENT);
        outbox.setRetryCount(2);
        syncOutboxRepository.save(outbox);
        entityManager.flush();

        // When - 再度失敗（retryCountが3になり、MAX_RETRYを超える）
        syncService.markSentAsFailed(transactionId, "Max retries exceeded");

        // Then - FAILED状態になる
        entityManager.flush();
        entityManager.clear();
        SyncOutbox updated = syncOutboxRepository.findByTransactionId(transactionId).orElseThrow();
        assertEquals(SyncOutbox.Status.FAILED, updated.getStatus(),
                "リトライ上限超過時はFAILED状態になるべき");
        assertEquals(3, updated.getRetryCount());
    }

    @Test
    void markSentAsFailed_SENT状態以外は無視すること() {
        // Given - PENDING状態のOutboxエントリを作成
        SyncOutbox outbox = new SyncOutbox();
        outbox.setTransactionId(transactionId);
        outbox.setPayload("{\"test\": true}");
        outbox.setStatus(SyncOutbox.Status.PENDING);
        outbox.setRetryCount(0);
        syncOutboxRepository.save(outbox);
        entityManager.flush();

        // When - markSentAsFailedを呼び出す（SENT状態ではない）
        syncService.markSentAsFailed(transactionId, "Error");

        // Then - 状態は変更されない
        entityManager.flush();
        entityManager.clear();
        SyncOutbox updated = syncOutboxRepository.findByTransactionId(transactionId).orElseThrow();
        assertEquals(SyncOutbox.Status.PENDING, updated.getStatus(),
                "SENT状態以外は無視されるべき");
        assertEquals(0, updated.getRetryCount(), "retryCountは変更されないべき");
    }

    @Test
    void markSentAsFailed_存在しないtransactionIdは無視すること() {
        // When - 存在しないtransactionIdでmarkSentAsFailedを呼び出す
        syncService.markSentAsFailed("NONEXISTENT", "Error");

        // Then - 例外が発生せず、何も起こらない
    }

    // === 入力検証テスト ===

    @Test
    void markAsSent_null取引IDでValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            syncService.markAsSent(null);
        });
    }

    @Test
    void markAsSent_空文字列の取引IDでValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            syncService.markAsSent("");
        });
    }

    @Test
    void markAsFailed_null取引IDでValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            syncService.markAsFailed(null, "error");
        });
    }

    @Test
    void queueInbox_nullイベントタイプでValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            syncService.queueInbox(null, "event-1", "payload");
        });
    }

    @Test
    void queueInbox_nullイベントIDでValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            syncService.queueInbox("PRODUCT_CREATED", null, "payload");
        });
    }

    @Test
    void markAsApplied_nullInboxIDでValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            syncService.markAsApplied(null);
        });
    }
}
