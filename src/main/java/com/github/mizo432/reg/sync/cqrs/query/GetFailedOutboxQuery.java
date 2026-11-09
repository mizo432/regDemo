package com.github.mizo432.reg.sync.cqrs.query;

import com.github.mizo432.reg.cqrs.Query;

import java.util.List;

/**
 * 失敗したOutbox取得クエリ
 */
public record GetFailedOutboxQuery() implements Query {
}
