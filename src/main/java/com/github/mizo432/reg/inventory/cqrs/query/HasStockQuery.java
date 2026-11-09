package com.github.mizo432.reg.inventory.cqrs.query;

import com.github.mizo432.reg.cqrs.Query;

/**
 * 在庫確認クエリ
 */
public record HasStockQuery(Long productId, int amount) implements Query {
}
