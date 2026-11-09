package com.github.mizo432.reg.product.cqrs.command;

import com.github.mizo432.reg.cqrs.Command;

/**
 * 商品削除コマンド
 */
public record DeleteProductCommand(Long id) implements Command {
}
