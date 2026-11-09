package com.github.mizo432.reg.inventory.cqrs.command;

import com.github.mizo432.reg.cqrs.Command;

/**
 * 在庫増加分コマンド
 */
public record IncreaseStockCommand(Long productId, int amount) implements Command {
}
