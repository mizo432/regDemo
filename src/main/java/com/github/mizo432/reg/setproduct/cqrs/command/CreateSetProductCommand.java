package com.github.mizo432.reg.setproduct.cqrs.command;

import com.github.mizo432.reg.cqrs.Command;

import java.math.BigDecimal;
import java.util.List;

/**
 * セット商品登録コマンド
 */
public record CreateSetProductCommand(
        String setBarcode,
        String setName,
        BigDecimal setPrice,
        List<SetProductItemCommand> items
) implements Command {

    /**
     * セット商品アイテムのコマンド
     */
    public record SetProductItemCommand(
            Long productId,
            int quantity
    ) {}
}
