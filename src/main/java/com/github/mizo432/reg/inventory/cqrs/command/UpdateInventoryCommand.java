package com.github.mizo432.reg.inventory.cqrs.command;

import com.github.mizo432.reg.cqrs.Command;

/**
 * 在庫更新コマンド
 */
public record UpdateInventoryCommand(Long productId, int quantity) implements Command {
}
