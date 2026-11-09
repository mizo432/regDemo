package com.github.mizo432.reg.commission.cqrs.command;

import com.github.mizo432.reg.cqrs.Command;

import java.math.BigDecimal;

/**
 * 委託商品登録コマンド
 */
public record CreateCommissionProductCommand(
        Long productId,
        String commissioner,
        BigDecimal commissionRate,
        String commissionType
) implements Command {
}
