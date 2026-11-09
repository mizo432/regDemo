package com.github.mizo432.reg.product.cqrs.query;

import com.github.mizo432.reg.cqrs.Query;

/**
 * IDで商品取得クエリ
 */
public record GetProductByIdQuery(Long id) implements Query {
}
