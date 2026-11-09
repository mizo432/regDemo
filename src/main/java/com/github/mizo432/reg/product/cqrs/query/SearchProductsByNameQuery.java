package com.github.mizo432.reg.product.cqrs.query;

import com.github.mizo432.reg.cqrs.Query;

/**
 * 商品名で検索クエリ
 */
public record SearchProductsByNameQuery(String name) implements Query {
}
