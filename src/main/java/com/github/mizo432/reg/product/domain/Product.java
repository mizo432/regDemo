package com.github.mizo432.reg.product.domain;

import com.github.mizo432.reg.exception.ValidationException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String barcode;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TaxCategory taxCategory = TaxCategory.TAXABLE;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

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

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            throw new ValidationException("barcode", "バーコードは空であってはいけません");
        }
        this.barcode = barcode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("name", "商品名は空であってはいけません");
        }
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        if (price == null) {
            throw new ValidationException("price", "価格はnullであってはいけません");
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("price", "価格は0以上である必要があります: " + price);
        }
        this.price = price;
    }

    public TaxCategory getTaxCategory() {
        return taxCategory;
    }

    public void setTaxCategory(TaxCategory taxCategory) {
        this.taxCategory = taxCategory;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public enum TaxCategory {
        TAXABLE, REDUCED, NON_TAXABLE
    }

    public enum Status {
        ACTIVE, INACTIVE, DISCONTINUED
    }
}
