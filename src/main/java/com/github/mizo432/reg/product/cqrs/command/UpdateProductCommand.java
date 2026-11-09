package com.github.mizo432.reg.product.cqrs.command;

import com.github.mizo432.reg.cqrs.Command;

import java.math.BigDecimal;

/**
 * 商品更新コマンド
 */
public record UpdateProductCommand(
        Long id,
        String name,
        String barcode,
        BigDecimal price,
        String taxCategory,
        String status
) implements Command {
}
