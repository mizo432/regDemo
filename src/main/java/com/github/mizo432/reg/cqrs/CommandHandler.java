package com.github.mizo432.reg.cqrs;

/**
 * コマンドハンドラーのインターフェース。
 * コマンドを実行し、結果を返します。
 *
 * @param <C> コマンド型
 * @param <R> 結果型
 */
public interface CommandHandler<C extends Command, R> {

    /**
     * コマンドを実行します。
     *
     * @param command 実行するコマンド
     * @return 実行結果
     */
    R execute(C command);

    /**
     * このハンドラーが指定されたコマンドを処理できるかどうかを確認します。
     */
    default boolean supports(Class<? extends Command> commandType) {
        return false;
    }
}
