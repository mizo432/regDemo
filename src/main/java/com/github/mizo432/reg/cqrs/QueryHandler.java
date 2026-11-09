package com.github.mizo432.reg.cqrs;

/**
 * クエリハンドラーのインターフェース。
 * クエリを実行し、結果を返します。
 *
 * @param <Q> クエリ型
 * @param <R> 結果型
 */
public interface QueryHandler<Q extends Query, R> {

    /**
     * クエリを実行します。
     *
     * @param query 実行するクエリ
     * @return クエリ結果
     */
    R handle(Q query);
}
