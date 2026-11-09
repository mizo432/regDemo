package com.github.mizo432.reg.commission.domain;

import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.product.domain.Product;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "commission_product")
public class CommissionProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(nullable = false, length = 200)
    private String commissioner;

    @Column(name = "commission_rate", precision = 5, scale = 4)
    private BigDecimal commissionRate;

    @Column(name = "fixed_commission", precision = 12, scale = 2)
    private BigDecimal fixedCommission;

    @Column(name = "commission_type", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private CommissionType commissionType = CommissionType.RATE;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

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

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getCommissioner() {
        return commissioner;
    }

    public void setCommissioner(String commissioner) {
        this.commissioner = commissioner;
    }

    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(BigDecimal commissionRate) {
        if (commissionRate != null && commissionRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("commissionRate", "手数料率は0以上である必要があります: " + commissionRate);
        }
        if (commissionRate != null && commissionRate.compareTo(BigDecimal.ONE) > 0) {
            throw new ValidationException("commissionRate", "手数料率は1.0（100%）以下である必要があります: " + commissionRate);
        }
        this.commissionRate = commissionRate;
    }

    public BigDecimal getFixedCommission() {
        return fixedCommission;
    }

    public void setFixedCommission(BigDecimal fixedCommission) {
        if (fixedCommission != null && fixedCommission.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("fixedCommission", "固定額は0以上である必要があります: " + fixedCommission);
        }
        this.fixedCommission = fixedCommission;
    }

    public CommissionType getCommissionType() {
        return commissionType;
    }

    public void setCommissionType(CommissionType commissionType) {
        this.commissionType = commissionType;
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

    public BigDecimal calculateCommission(BigDecimal saleAmount) {
        if (saleAmount == null) {
            throw new ValidationException("saleAmount", "販売額はnullであってはいけません");
        }
        if (saleAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("saleAmount", "販売額は0以上である必要があります: " + saleAmount);
        }
        if (commissionType == CommissionType.RATE) {
            if (commissionRate == null) {
                throw new ValidationException("commissionRate", "RATEタイプの委託商品では手数料率が設定されている必要があります");
            }
            if (commissionRate.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("commissionRate", "手数料率は0以上である必要があります: " + commissionRate);
            }
            if (commissionRate.compareTo(BigDecimal.ONE) > 0) {
                throw new ValidationException("commissionRate", "手数料率は1.0（100%）以下である必要があります: " + commissionRate);
            }
            return saleAmount.multiply(commissionRate)
                    .setScale(2, RoundingMode.HALF_UP);
        } else if (commissionType == CommissionType.FIXED) {
            if (fixedCommission == null) {
                throw new ValidationException("fixedCommission", "FIXEDタイプの委託商品では固定額が設定されている必要があります");
            }
            if (fixedCommission.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("fixedCommission", "固定額は0以上である必要があります: " + fixedCommission);
            }
            return fixedCommission;
        }
        return BigDecimal.ZERO;
    }

    public enum CommissionType {
        RATE, FIXED
    }

    public enum Status {
        ACTIVE, INACTIVE
    }
}
