package com.github.mizo432.reg.inventory.cqrs.command;

import com.github.mizo432.reg.cqrs.Command;

/**
 * 在庫予約解除コマンド
 */
public record ReleaseReservedStockCommand(Long productId, int amount) implements Command {
}
