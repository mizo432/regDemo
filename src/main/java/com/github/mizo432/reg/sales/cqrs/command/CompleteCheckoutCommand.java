package com.github.mizo432.reg.sales.cqrs.command;

import com.github.mizo432.reg.cqrs.Command;
import com.github.mizo432.reg.sales.domain.Transaction;

/**
 * 取引完了コマンド
 */
public record CompleteCheckoutCommand(Transaction transaction) implements Command {
}
