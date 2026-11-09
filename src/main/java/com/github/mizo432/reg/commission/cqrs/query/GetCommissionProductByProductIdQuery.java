package com.github.mizo432.reg.commission.cqrs.query;

import com.github.mizo432.reg.cqrs.Query;

/**
 * 商品IDで委託商品取得クエリ
 */
public record GetCommissionProductByProductIdQuery(Long productId) implements Query {
}
