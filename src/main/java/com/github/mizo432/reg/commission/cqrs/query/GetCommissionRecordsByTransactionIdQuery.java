package com.github.mizo432.reg.commission.cqrs.query;

import com.github.mizo432.reg.cqrs.Query;

import java.util.List;

/**
 * 取引IDで委託記録取得クエリ
 */
public record GetCommissionRecordsByTransactionIdQuery(Long transactionId) implements Query {
}
