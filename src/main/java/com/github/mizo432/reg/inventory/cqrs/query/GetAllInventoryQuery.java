package com.github.mizo432.reg.inventory.cqrs.query;

import com.github.mizo432.reg.cqrs.Query;

import java.util.List;

/**
 * 全在庫取得クエリ
 */
public record GetAllInventoryQuery() implements Query {
}
