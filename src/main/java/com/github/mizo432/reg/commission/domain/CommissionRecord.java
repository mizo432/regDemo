package com.github.mizo432.reg.commission.domain;

import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.product.domain.Product;
import com.github.mizo432.reg.sales.domain.Transaction;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "commission_record")
public class CommissionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "sale_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal saleAmount;

    @Column(name = "commission_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "payable_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal payableAmount;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public BigDecimal getSaleAmount() {
        return saleAmount;
    }

    public void setSaleAmount(BigDecimal saleAmount) {
        if (saleAmount != null && saleAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("saleAmount", "販売額は0以上である必要があります: " + saleAmount);
        }
        this.saleAmount = saleAmount;
    }

    public BigDecimal getCommissionAmount() {
        return commissionAmount;
    }

    public void setCommissionAmount(BigDecimal commissionAmount) {
        if (commissionAmount != null && commissionAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("commissionAmount", "手数料額は0以上である必要があります: " + commissionAmount);
        }
        this.commissionAmount = commissionAmount;
    }

    public BigDecimal getPayableAmount() {
        return payableAmount;
    }

    public void setPayableAmount(BigDecimal payableAmount) {
        if (payableAmount != null && payableAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("payableAmount", "支払額は0以上である必要があります: " + payableAmount);
        }
        this.payableAmount = payableAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
