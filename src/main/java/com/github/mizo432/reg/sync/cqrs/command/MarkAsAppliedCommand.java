package com.github.mizo432.reg.sync.cqrs.command;

import com.github.mizo432.reg.cqrs.Command;

/**
 * Inboxを適用済みとしてマークするコマンド
 */
public record MarkAsAppliedCommand(Long inboxId) implements Command {
}
