package com.github.mizo432.reg.product.domain;

import com.github.mizo432.reg.exception.ValidationException;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "set_product_item")
public class SetProductItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_product_id", nullable = false)
    private SetProduct setProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity = 1;

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

    public SetProduct getSetProduct() {
        return setProduct;
    }

    public void setSetProduct(SetProduct setProduct) {
        this.setProduct = setProduct;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity < 0) {
            throw new ValidationException("quantity", "数量は0以上である必要があります: " + quantity);
        }
        this.quantity = quantity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
