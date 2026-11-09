package com.github.mizo432.reg.setproduct.cqrs.command;

import com.github.mizo432.reg.cqrs.Command;

/**
 * セット商品削除コマンド
 */
public record DeleteSetProductCommand(Long id) implements Command {
}
