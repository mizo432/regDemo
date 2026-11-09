package com.github.mizo432.reg.product.cqrs.command;

import com.github.mizo432.reg.cqrs.Command;

import java.math.BigDecimal;

/**
 * 商品登録コマンド
 */
public record CreateProductCommand(
        String name,
        String barcode,
        BigDecimal price,
        String taxCategory
) implements Command {
}
