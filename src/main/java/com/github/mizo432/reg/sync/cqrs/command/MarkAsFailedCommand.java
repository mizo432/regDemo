package com.github.mizo432.reg.sync.cqrs.command;

import com.github.mizo432.reg.cqrs.Command;

/**
 * 失敗としてマークするコマンド
 */
public record MarkAsFailedCommand(String transactionId, String error) implements Command {
}
