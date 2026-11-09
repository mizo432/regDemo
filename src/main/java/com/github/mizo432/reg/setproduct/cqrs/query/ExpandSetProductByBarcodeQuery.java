package com.github.mizo432.reg.setproduct.cqrs.query;

import com.github.mizo432.reg.cqrs.Query;

/**
 * バーコードでセット商品展開クエリ
 */
public record ExpandSetProductByBarcodeQuery(String barcode) implements Query {
}
