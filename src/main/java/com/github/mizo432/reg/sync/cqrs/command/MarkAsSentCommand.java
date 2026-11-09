package com.github.mizo432.reg.sync.cqrs.command;

import com.github.mizo432.reg.cqrs.Command;

/**
 * 送信済みとしてマークするコマンド
 */
public record MarkAsSentCommand(String transactionId) implements Command {
}
