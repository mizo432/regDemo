package com.github.mizo432.reg.product.service;

import com.github.mizo432.reg.product.domain.Product;

import java.math.BigDecimal;
import java.util.List;

/**
 * セット商品の展開結果を表すレコードです。
 */
public record SetProductExpansion(
        String setName,
        BigDecimal setPrice,
        List<ExpandedItem> items
) {
    /**
     * 展開された個別商品を表すレコードです。
     */
    public record ExpandedItem(
            Long productId,
            String barcode,
            String name,
            BigDecimal unitPrice,
            int quantity,
            Product.TaxCategory taxCategory
    ) {}
}
