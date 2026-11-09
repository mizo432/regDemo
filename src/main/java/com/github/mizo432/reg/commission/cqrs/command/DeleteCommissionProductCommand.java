package com.github.mizo432.reg.commission.cqrs.command;

import com.github.mizo432.reg.cqrs.Command;

/**
 * 委託商品削除コマンド
 */
public record DeleteCommissionProductCommand(Long id) implements Command {
}
