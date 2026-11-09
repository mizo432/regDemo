package com.github.mizo432.reg.inventory.domain;

import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.product.domain.Product;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(nullable = false)
    private int quantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity = 0;

    @Version
    private Long version = 0L;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
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
            throw new ValidationException("quantity", "在庫数量は0以上である必要があります: " + quantity);
        }
        this.quantity = quantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(int reservedQuantity) {
        if (reservedQuantity < 0) {
            throw new ValidationException("reservedQuantity", "予約数量は0以上である必要があります: " + reservedQuantity);
        }
        this.reservedQuantity = reservedQuantity;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public int getAvailableQuantity() {
        return quantity - reservedQuantity;
    }

    public boolean hasStock(int amount) {
        return getAvailableQuantity() >= amount;
    }

    public void decreaseStock(int amount) {
        if (amount < 0) {
            throw new ValidationException("amount", "減算数量は0以上である必要があります: " + amount);
        }
        if (amount > getAvailableQuantity()) {
            throw new IllegalStateException("在庫不足: 要求=" + amount + ", 利用可能=" + getAvailableQuantity());
        }
        this.quantity -= amount;
    }

    public void increaseStock(int amount) {
        if (amount < 0) {
            throw new ValidationException("amount", "増加分は0以上である必要があります: " + amount);
        }
        this.quantity += amount;
    }

    /**
     * 指定数量を予約する（reservedQuantityを増やす）。
     * カートに商品を追加した時点で在庫を予約し、他の顧客が購入できないようにする。
     */
    public void reserveStock(int amount) {
        if (amount < 0) {
            throw new ValidationException("amount", "予約数量は0以上である必要があります: " + amount);
        }
        if (amount > getAvailableQuantity()) {
            throw new IllegalStateException("在庫不足: 予約要求=" + amount + ", 利用可能=" + getAvailableQuantity());
        }
        this.reservedQuantity += amount;
    }

    /**
     * 指定数量の予約を解除する（reservedQuantityを減らす）。
     * カートから商品を削除したときや、チェックアウトで予約を確定したときに使用する。
     */
    public void releaseReservedStock(int amount) {
        if (amount < 0) {
            throw new ValidationException("amount", "予約解除数量は0以上である必要があります: " + amount);
        }
        if (amount > this.reservedQuantity) {
            throw new IllegalStateException("予約数量不足: 解除要求=" + amount + ", 予約中=" + this.reservedQuantity);
        }
        this.reservedQuantity -= amount;
    }

    /**
     * 予約を確定する（reservedQuantityを減らす）。
     * チェックアウト完了時に予約していた在庫を確定する。
     * reserveStock()ではquantityは減算されずreservedQuantityのみが増加するため、
     * confirmReservedStock()ではreservedQuantityのみを減算する。
     * これにより、quantity - reservedQuantity = availableQuantity の関係が保たれる。
     * 予約数量が不足する場合は、予約分を解除し、残りを直接quantityから減算する（後方互換性のため）。
     */
    public void confirmReservedStock(int amount) {
        if (amount < 0) {
            throw new ValidationException("amount", "予約確定数量は0以上である必要があります: " + amount);
        }
        if (amount > this.reservedQuantity) {
            // 予約数量が不足する場合、予約分を解除し、残りを直接quantityから減算
            int reservedPart = this.reservedQuantity;
            int remaining = amount - this.reservedQuantity;
            this.reservedQuantity = 0;
            // 予約不足分をquantityから減算
            this.quantity -= remaining;
            if (this.quantity < 0) {
                // 在庫不足: 変更を元に戻す
                this.quantity += remaining;
                this.reservedQuantity = reservedPart;
                int available = getAvailableQuantity();
                throw new IllegalStateException("在庫不足: 要求=" + amount + ", 利用可能=" + available);
            }
        } else {
            // 予約数量が十分にある場合 — reservedQuantityのみを減算
            // quantityは変更しない（reserveStock()でquantityは減算されていないため）
            this.reservedQuantity -= amount;
        }
    }
}
