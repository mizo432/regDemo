package com.github.mizo432.reg.sync.cqrs.command;

import com.github.mizo432.reg.cqrs.Command;

/**
 * Inboxイベントをキューに追加するコマンド
 */
public record QueueInboxCommand(String eventType, String eventId, String payload) implements Command {
}
