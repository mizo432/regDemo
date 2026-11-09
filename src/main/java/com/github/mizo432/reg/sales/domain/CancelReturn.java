package com.github.mizo432.reg.sales.domain;

import com.github.mizo432.reg.exception.ValidationException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cancel_return")
public class CancelReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_transaction_id", nullable = false, length = 50)
    private String originalTransactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(name = "return_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal returnAmount;

    @Column(length = 500)
    private String reason;

    @Column(name = "operator_id", length = 50)
    private String operatorId;

    @Column(name = "terminal_id", length = 50)
    private String terminalId;

    @Column(name = "date_time", nullable = false)
    private LocalDateTime dateTime;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (dateTime == null) {
            dateTime = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalTransactionId() {
        return originalTransactionId;
    }

    public void setOriginalTransactionId(String originalTransactionId) {
        this.originalTransactionId = originalTransactionId;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public BigDecimal getReturnAmount() {
        return returnAmount;
    }

    public void setReturnAmount(BigDecimal returnAmount) {
        if (returnAmount != null && returnAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("returnAmount", "返金額は0以上である必要があります: " + returnAmount);
        }
        this.returnAmount = returnAmount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public enum Status {
        PENDING, APPROVED, REJECTED
    }
}
