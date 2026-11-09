package com.github.mizo432.reg.sales.cqrs.query;

import com.github.mizo432.reg.cqrs.Query;

/**
 * キャンセル/返品記録取得クエリ
 */
public record GetCancelReturnsByTransactionIdQuery(String originalTransactionId) implements Query {
}
