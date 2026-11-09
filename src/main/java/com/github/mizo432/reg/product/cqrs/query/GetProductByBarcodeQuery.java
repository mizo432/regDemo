package com.github.mizo432.reg.product.cqrs.query;

import com.github.mizo432.reg.cqrs.Query;

/**
 * バーコードで商品取得クエリ
 */
public record GetProductByBarcodeQuery(String barcode) implements Query {
}
