package com.github.mizo432.reg.product.domain;

import com.github.mizo432.reg.exception.ValidationException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "set_product")
public class SetProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "set_barcode", nullable = false, unique = true, length = 50)
    private String setBarcode;

    @Column(name = "set_name", nullable = false, length = 200)
    private String setName;

    @Column(name = "set_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal setPrice;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    @Version
    private Long version = 0L;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "setProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SetProductItem> items = new ArrayList<>();

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

    public String getSetBarcode() {
        return setBarcode;
    }

    public void setSetBarcode(String setBarcode) {
        this.setBarcode = setBarcode;
    }

    public String getSetName() {
        return setName;
    }

    public void setSetName(String setName) {
        this.setName = setName;
    }

    public BigDecimal getSetPrice() {
        return setPrice;
    }

    public void setSetPrice(BigDecimal setPrice) {
        if (setPrice != null && setPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("setPrice", "セット価格は0以上である必要があります: " + setPrice);
        }
        this.setPrice = setPrice;
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

    public List<SetProductItem> getItems() {
        return items;
    }

    public void setItems(List<SetProductItem> items) {
        this.items = items;
    }

    public void addItem(SetProductItem item) {
        if (item == null) {
            throw new ValidationException("item", "アイテムはnullであってはいけません");
        }
        items.add(item);
        item.setSetProduct(this);
    }

    public enum Status {
        ACTIVE, INACTIVE
    }
}
