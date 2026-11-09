package com.github.mizo432.reg.inventory.cqrs.command;

import com.github.mizo432.reg.cqrs.Command;

/**
 * 在庫予約コマンド
 */
public record ReserveStockCommand(Long productId, int amount) implements Command {
}
