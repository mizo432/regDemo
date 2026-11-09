package com.github.mizo432.reg.sync.cqrs.query;

import com.github.mizo432.reg.cqrs.Query;

import java.util.List;

/**
 * 送信済みOutbox取得クエリ
 */
public record GetSentOutboxQuery() implements Query {
}
