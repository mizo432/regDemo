package com.github.mizo432.reg.sales.domain;

import com.github.mizo432.reg.exception.ValidationException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transaction")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 50)
    private String transactionId;

    @Column(name = "date_time", nullable = false)
    private LocalDateTime dateTime;

    @Column(name = "terminal_id", length = 50)
    private String terminalId;

    @Column(name = "operator_id", length = 50)
    private String operatorId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxTotal = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(name = "payment_method", length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(name = "paid_amount", precision = 12, scale = 2)
    private BigDecimal paidAmount;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status = Status.COMPLETED;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<TransactionItem> items = new ArrayList<>();

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

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        if (subtotal != null && subtotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("subtotal", "小計は0以上である必要があります: " + subtotal);
        }
        this.subtotal = subtotal;
    }

    public BigDecimal getTaxTotal() {
        return taxTotal;
    }

    public void setTaxTotal(BigDecimal taxTotal) {
        if (taxTotal != null && taxTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("taxTotal", "税額は0以上である必要があります: " + taxTotal);
        }
        this.taxTotal = taxTotal;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(BigDecimal grandTotal) {
        if (grandTotal != null && grandTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("grandTotal", "合計は0以上である必要があります: " + grandTotal);
        }
        this.grandTotal = grandTotal;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
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

    public Long getVersion() {
        return version;
    }

    public List<TransactionItem> getItems() {
        return items;
    }

    public void setItems(List<TransactionItem> items) {
        this.items = items;
    }

    public void addItem(TransactionItem item) {
        if (item == null) {
            throw new ValidationException("item", "アイテムはnullであってはいけません");
        }
        items.add(item);
        item.setTransaction(this);
    }

    public enum PaymentMethod {
        CASH, CARD, VOUCHER, OTHER
    }

    public enum Status {
        COMPLETED, CANCELLED, RETURNED
    }
}
