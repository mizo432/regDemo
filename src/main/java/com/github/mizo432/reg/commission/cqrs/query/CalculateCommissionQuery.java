package com.github.mizo432.reg.commission.cqrs.query;

import com.github.mizo432.reg.cqrs.Query;

import java.math.BigDecimal;

/**
 * 手数料計算クエリ
 */
public record CalculateCommissionQuery(Long productId, BigDecimal saleAmount) implements Query {
}
