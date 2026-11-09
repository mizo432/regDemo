package com.github.mizo432.reg.sync.cqrs.query;

import com.github.mizo432.reg.cqrs.Query;

import java.util.List;

/**
 * 保留中のOutbox取得クエリ
 */
public record GetPendingOutboxQuery() implements Query {
}
