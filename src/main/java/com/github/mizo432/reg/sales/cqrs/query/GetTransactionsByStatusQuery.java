package com.github.mizo432.reg.sales.cqrs.query;

import com.github.mizo432.reg.cqrs.Query;
import com.github.mizo432.reg.sales.domain.Transaction;

/**
 * 状態別取引取得クエリ
 */
public record GetTransactionsByStatusQuery(Transaction.Status status) implements Query {
}
