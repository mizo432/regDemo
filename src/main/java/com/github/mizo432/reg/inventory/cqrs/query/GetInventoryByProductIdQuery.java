package com.github.mizo432.reg.inventory.cqrs.query;

import com.github.mizo432.reg.cqrs.Query;

/**
 * 商品IDで在庫取得クエリ
 */
public record GetInventoryByProductIdQuery(Long productId) implements Query {
}
