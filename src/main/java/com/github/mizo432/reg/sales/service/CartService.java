package com.github.mizo432.reg.sales.service;

import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.inventory.service.InventoryService;
import com.github.mizo432.reg.product.domain.Product;
import com.github.mizo432.reg.product.service.ProductService;
import com.github.mizo432.reg.product.service.SetProductExpansion;
import com.github.mizo432.reg.product.service.SetProductService;
import com.github.mizo432.reg.sales.domain.Transaction;
import com.github.mizo432.reg.sales.domain.TransactionItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class CartService {

    private final ProductService productService;
    private final SetProductService setProductService;
    private final InventoryService inventoryService;
    private final BigDecimal taxRate;
    private final BigDecimal reducedTaxRate;

    public CartService(ProductService productService,
                       SetProductService setProductService,
                       InventoryService inventoryService,
                       @Value("${reg.tax.rate:0.1}") BigDecimal taxRate,
                       @Value("${reg.tax.reducedRate:0.08}") BigDecimal reducedTaxRate) {
        this.productService = productService;
        this.setProductService = setProductService;
        this.inventoryService = inventoryService;
        this.taxRate = taxRate;
        this.reducedTaxRate = reducedTaxRate;
    }

    public Transaction createCart() {
        Transaction transaction = new Transaction();
        transaction.setDateTime(java.time.LocalDateTime.now());
        return transaction;
    }

    public void addToCart(Transaction transaction, String barcode, int quantity) {
        // 入力検証
        if (barcode == null || barcode.trim().isEmpty()) {
            throw new ValidationException("barcode", "バーコードは空であってはいけません");
        }
        if (quantity <= 0) {
            throw new ValidationException("quantity", "数量は1以上である必要があります: " + quantity);
        }

        // まず通常商品として検索
        Product product = productService.findByBarcode(barcode)
                .orElse(null);

        if (product != null) {
            addProductToCart(transaction, product, quantity);
            return;
        }

        // セット商品として検索
        SetProductExpansion expansion = setProductService.expandByBarcode(barcode)
                .orElseThrow(() -> new IllegalArgumentException("商品が見つかりません: " + barcode));

        // セット商品の内包商品を展開して追加
        // 配分後の価格（expandedItem.unitPrice()）を使用し、セット商品の割引を反映
        for (SetProductExpansion.ExpandedItem expandedItem : expansion.items()) {
            Product expandedProduct = productService.findByBarcode(expandedItem.barcode())
                    .orElseThrow(() -> new IllegalArgumentException("セット商品に含まれる商品が見つかりません: " + expandedItem.barcode()));
            // 整数オーバーフローをチェック
            int expandedQuantity = Math.multiplyExact(expandedItem.quantity(), quantity);
            addProductToCart(transaction, expandedProduct, expandedQuantity, expandedItem.unitPrice());
        }
    }

    private void addProductToCart(Transaction transaction, Product product, int quantity) {
        addProductToCart(transaction, product, quantity, null);
    }

    private void addProductToCart(Transaction transaction, Product product, int quantity, BigDecimal unitPrice) {
        // unitPriceが指定されている場合はそれを使用（セット商品の配分後価格）、
        // 否则はタイムセール等を考慮した有効価格を取得
        BigDecimal effectivePrice = unitPrice != null
                ? unitPrice
                : productService.getEffectivePrice(product.getId()).orElse(product.getPrice());

        // 既存の明細に追加（単価が同じ場合のみ）
        for (TransactionItem item : transaction.getItems()) {
            if (item.getProduct().getId().equals(product.getId())) {
                // 単価が同じ場合は数量を追加
                if (item.getUnitPrice().compareTo(effectivePrice) == 0) {
                    item.setQuantity(item.getQuantity() + quantity);
                    // 追加分のみ在庫を予約
                    inventoryService.reserveStock(product.getId(), quantity);
                    return;
                }
                // 単価が異なる場合は新しい明細として追加（breakして下に進む）
                break;
            }
        }

        // 新しい明細を追加
        TransactionItem item = new TransactionItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnitPrice(effectivePrice);
        transaction.addItem(item);
        // 在庫を予約
        inventoryService.reserveStock(product.getId(), quantity);
    }

    /**
     * productIdとunitPriceで特定した明細を削除する。
     * NC3修正により、同じ商品が異なる単価で複数の明細として存在するため、
     * 単価も含めて特定する必要がある。
     */
    public void removeFromCart(Transaction transaction, Long productId, BigDecimal unitPrice) {
        // 削除する前に予約数量を収集
        int totalQuantity = 0;
        for (TransactionItem item : transaction.getItems()) {
            if (item.getProduct().getId().equals(productId) 
                && item.getUnitPrice().compareTo(unitPrice) == 0) {
                totalQuantity += item.getQuantity();
            }
        }
        transaction.getItems().removeIf(item -> 
            item.getProduct().getId().equals(productId) 
            && item.getUnitPrice().compareTo(unitPrice) == 0
        );
        // 予約を解除
        if (totalQuantity > 0) {
            inventoryService.releaseReservedStock(productId, totalQuantity);
        }
    }

    /**
     * productIdのみで削除する（後方互換用）。同じ商品が異なる単価で複数の明細として
     * 存在する場合、全明細が削除されるため注意すること。
     * @deprecated {@link #removeFromCart(Transaction, Long, BigDecimal)} を使用すること
     */
    @Deprecated
    public void removeFromCart(Transaction transaction, Long productId) {
        // 削除する前に予約数量を収集
        int totalQuantity = 0;
        for (TransactionItem item : transaction.getItems()) {
            if (item.getProduct().getId().equals(productId)) {
                totalQuantity += item.getQuantity();
            }
        }
        transaction.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        // 予約を解除
        if (totalQuantity > 0) {
            inventoryService.releaseReservedStock(productId, totalQuantity);
        }
    }

    /**
     * productIdとunitPriceで特定した明細の数量を更新する。
     * NC3修正により、同じ商品が異なる単価で複数の明細として存在するため、
     * 単価も含めて特定する必要がある。
     */
    public void updateQuantity(Transaction transaction, Long productId, BigDecimal unitPrice, int quantity) {
        if (quantity < 0) {
            throw new ValidationException("quantity", "数量は0以上である必要があります: " + quantity);
        }
        for (TransactionItem item : transaction.getItems()) {
            if (item.getProduct().getId().equals(productId) 
                && item.getUnitPrice().compareTo(unitPrice) == 0) {
                if (quantity <= 0) {
                    removeFromCart(transaction, productId, unitPrice);
                } else {
                    int oldQuantity = item.getQuantity();
                    item.setQuantity(quantity);
                    // 予約数量を更新
                    if (quantity > oldQuantity) {
                        // 数量が増えた場合、追加分を予約
                        inventoryService.reserveStock(productId, quantity - oldQuantity);
                    } else if (quantity < oldQuantity) {
                        // 数量が減った場合、差分の予約を解除
                        inventoryService.releaseReservedStock(productId, oldQuantity - quantity);
                    }
                }
                return;
            }
        }
    }

    /**
     * productIdのみで数量を更新する（後方互換用）。同じ商品が異なる単価で複数の明細として
     * 存在する場合、最初の明細のみが更新されるため注意すること。
     * @deprecated {@link #updateQuantity(Transaction, Long, BigDecimal, int)} を使用すること
     */
    @Deprecated
    public void updateQuantity(Transaction transaction, Long productId, int quantity) {
        if (quantity < 0) {
            throw new ValidationException("quantity", "数量は0以上である必要があります: " + quantity);
        }
        for (TransactionItem item : transaction.getItems()) {
            if (item.getProduct().getId().equals(productId)) {
                if (quantity <= 0) {
                    removeFromCart(transaction, productId);
                } else {
                    int oldQuantity = item.getQuantity();
                    item.setQuantity(quantity);
                    // 予約数量を更新
                    if (quantity > oldQuantity) {
                        inventoryService.reserveStock(productId, quantity - oldQuantity);
                    } else if (quantity < oldQuantity) {
                        inventoryService.releaseReservedStock(productId, oldQuantity - quantity);
                    }
                }
                return;
            }
        }
    }

    public BigDecimal calculateSubtotal(Transaction transaction) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (TransactionItem item : transaction.getItems()) {
            subtotal = subtotal.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return subtotal;
    }

    public BigDecimal calculateTax(Transaction transaction) {
        BigDecimal taxTotal = BigDecimal.ZERO;

        for (TransactionItem item : transaction.getItems()) {
            BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            
            // 税区分を考慮して課税
            Product.TaxCategory taxCategory = item.getProduct().getTaxCategory();
            if (taxCategory == Product.TaxCategory.TAXABLE) {
                BigDecimal tax = itemTotal.multiply(taxRate)
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                taxTotal = taxTotal.add(tax);
            } else if (taxCategory == Product.TaxCategory.REDUCED) {
                BigDecimal tax = itemTotal.multiply(reducedTaxRate)
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                taxTotal = taxTotal.add(tax);
            }
            // NON_TAXABLEの場合は税金を計算しない
        }
        return taxTotal;
    }

    public BigDecimal calculateTotal(Transaction transaction) {
        BigDecimal subtotal = calculateSubtotal(transaction);
        BigDecimal tax = calculateTax(transaction);
        return subtotal.add(tax);
    }

    public List<CartLine> getCartLines(Transaction transaction) {
        List<CartLine> lines = new ArrayList<>();
        for (TransactionItem item : transaction.getItems()) {
            lines.add(new CartLine(
                    item.getProduct().getId(),
                    item.getProduct().getBarcode(),
                    item.getProduct().getName(),
                    item.getUnitPrice(),
                    item.getQuantity(),
                    item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
            ));
        }
        return lines;
    }

    public record CartLine(Long productId, String barcode, String name, BigDecimal unitPrice, int quantity, BigDecimal total) {}

    /**
     * 指定された商品IDと数量分の予約在庫を解除する。
     * カートクリア時など、明細を削除せずに予約のみを解除する必要がある場合に使用する。
     */
    public void releaseItemReservation(Long productId, int quantity) {
        if (quantity > 0) {
            inventoryService.releaseReservedStock(productId, quantity);
        }
    }
}
