package com.github.mizo432.reg.sales.cqrs.command;

import com.github.mizo432.reg.cqrs.Command;

/**
 * 取引キャンセルコマンド
 */
public record CancelTransactionCommand(String originalTransactionId, String reason, String operatorId) implements Command {
}
