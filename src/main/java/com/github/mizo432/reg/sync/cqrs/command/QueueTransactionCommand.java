package com.github.mizo432.reg.sync.cqrs.command;

import com.github.mizo432.reg.cqrs.Command;

/**
 * 取引を同期キューに追加するコマンド
 */
public record QueueTransactionCommand(String transactionId, String payload) implements Command {
}
