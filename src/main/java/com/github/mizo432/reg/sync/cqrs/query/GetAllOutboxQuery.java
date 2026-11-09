package com.github.mizo432.reg.sync.cqrs.query;

import com.github.mizo432.reg.cqrs.Query;

import java.util.List;

/**
 * 全Outbox取得クエリ
 */
public record GetAllOutboxQuery() implements Query {
}
