package com.github.mizo432.reg.product.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

@Entity
@Table(name = "time_sale")
public class TimeSale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "sale_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

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

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        if (salePrice == null) {
            throw new IllegalArgumentException("セール価格はnullであってはいけません");
        }
        if (salePrice.compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("セール価格は0以上である必要があります: " + salePrice);
        }
        this.salePrice = salePrice;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("開始日はnullであってはいけません");
        }
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        if (endDate == null) {
            throw new IllegalArgumentException("終了日はnullであってはいけません");
        }
        this.endDate = endDate;
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

    /**
     * 現在時刻（システムクロック）を使用して、このタイムセールがアクティブかどうかを判定する。
     */
    public boolean isActiveNow() {
        return isActiveNow(Clock.systemDefaultZone());
    }

    /**
     * 指定されたClockを使用して、このタイムセールがアクティブかどうかを判定する。
     * テスト時に時間を固定するために使用する。
     */
    public boolean isActiveNow(Clock clock) {
        LocalDateTime now = LocalDateTime.now(clock);
        return status == Status.ACTIVE && now.isAfter(startDate) && !now.isAfter(endDate);
    }

    public enum Status {
        ACTIVE, INACTIVE, EXPIRED
    }
}
