package com.github.mizo432.reg.sync.domain;

import com.github.mizo432.reg.exception.ValidationException;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sync_outbox")
public class SyncOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 50)
    private String transactionId;

    @Lob
    @Column(nullable = false, columnDefinition = "CLOB")
    private String payload;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    public static final int MAX_RETRY = 3;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    public boolean isExhausted() {
        return retryCount >= MAX_RETRY;
    }

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new ValidationException("transactionId", "トランザクションIDは空であってはいけません");
        }
        this.transactionId = transactionId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            throw new ValidationException("payload", "ペイロードは空であってはいけません");
        }
        this.payload = payload;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        if (retryCount < 0) {
            throw new ValidationException("retryCount", "リトライ回数は0以上である必要があります: " + retryCount);
        }
        this.retryCount = retryCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public enum Status {
        PENDING, SENT, FAILED
    }
}
