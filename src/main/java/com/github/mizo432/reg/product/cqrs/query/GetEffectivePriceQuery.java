package com.github.mizo432.reg.product.cqrs.query;

import com.github.mizo432.reg.cqrs.Query;

/**
 * 商品の実効価格取得クエリ（タイムセール等を考慮）
 */
public record GetEffectivePriceQuery(Long productId) implements Query {
}
