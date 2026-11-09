package com.github.mizo432.reg.product.cqrs.query;

import com.github.mizo432.reg.cqrs.Query;

/**
 * 有効な商品一覧取得クエリ
 */
public record GetActiveProductsQuery() implements Query {
}
