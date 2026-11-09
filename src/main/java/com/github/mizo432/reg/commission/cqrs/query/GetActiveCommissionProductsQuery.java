package com.github.mizo432.reg.commission.cqrs.query;

import com.github.mizo432.reg.cqrs.Query;

import java.util.List;

/**
 * 有効な委託商品一覧取得クエリ
 */
public record GetActiveCommissionProductsQuery() implements Query {
}
