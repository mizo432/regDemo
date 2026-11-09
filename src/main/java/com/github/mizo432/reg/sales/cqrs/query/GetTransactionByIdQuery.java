package com.github.mizo432.reg.sales.cqrs.query;

import com.github.mizo432.reg.cqrs.Query;

/**
 * 取引IDで取引取得クエリ
 */
public record GetTransactionByIdQuery(String transactionId) implements Query {
}
