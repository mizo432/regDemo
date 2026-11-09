package com.github.mizo432.reg.inventory.cqrs.handler;

import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.inventory.cqrs.query.GetAllInventoryQuery;
import com.github.mizo432.reg.inventory.cqrs.query.GetInventoryByProductIdQuery;
import com.github.mizo432.reg.inventory.cqrs.query.HasStockQuery;
import com.github.mizo432.reg.inventory.domain.Inventory;
import com.github.mizo432.reg.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 在庫関連のクエリを処理するハンドラー
 */
@Component
@Transactional(readOnly = true)
public class InventoryQueryHandler {

    private final InventoryRepository inventoryRepository;

    public InventoryQueryHandler(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public List<Inventory> handle(GetAllInventoryQuery query) {
        return inventoryRepository.findAll();
    }

    public Optional<Inventory> handle(GetInventoryByProductIdQuery query) {
        if (query.productId() == null) {
            throw new ValidationException("productId", "商品IDはnullであってはいけません");
        }
        return inventoryRepository.findByProductId(query.productId());
    }

    public Boolean handle(HasStockQuery query) {
        if (query.productId() == null) {
            throw new ValidationException("productId", "商品IDはnullであってはいけません");
        }
        if (query.amount() < 0) {
            throw new ValidationException("amount", "数量は0以上である必要があります: " + query.amount());
        }
        return inventoryRepository.findByProductId(query.productId())
                .map(inv -> inv.hasStock(query.amount()))
                .orElse(false);
    }
}
