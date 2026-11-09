package com.github.mizo432.reg.setproduct.cqrs.query;

import com.github.mizo432.reg.cqrs.Query;

/**
 * IDでセット商品取得クエリ
 */
public record GetSetProductByIdQuery(Long id) implements Query {
}
