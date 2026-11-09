package com.github.mizo432.reg.commission.cqrs.command;

import com.github.mizo432.reg.cqrs.Command;

/**
 * 委託商品保存コマンド
 */
public record SaveCommissionProductCommand(Long id) implements Command {
}
