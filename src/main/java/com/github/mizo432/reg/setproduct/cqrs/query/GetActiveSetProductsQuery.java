package com.github.mizo432.reg.setproduct.cqrs.query;

import com.github.mizo432.reg.cqrs.Query;

import java.util.List;

/**
 * 有効なセット商品一覧取得クエリ
 */
public record GetActiveSetProductsQuery() implements Query {
}
