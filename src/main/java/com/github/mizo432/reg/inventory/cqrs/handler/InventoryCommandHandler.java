package com.github.mizo432.reg.inventory.cqrs.handler;

import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.inventory.cqrs.command.*;
import com.github.mizo432.reg.inventory.domain.Inventory;
import com.github.mizo432.reg.inventory.repository.InventoryRepository;
import com.github.mizo432.reg.product.repository.ProductRepository;
import jakarta.persistence.OptimisticLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 在庫関連のコマンドを処理するハンドラー
 */
@Component
@Transactional
public class InventoryCommandHandler {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public InventoryCommandHandler(InventoryRepository inventoryRepository, ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    public Inventory execute(UpdateInventoryCommand command) {
        if (command.quantity() < 0) {
            throw new ValidationException("quantity", "在庫数量は0以上である必要があります: " + command.quantity());
        }
        var product = productRepository.findById(command.productId())
                .orElseThrow(() -> new IllegalArgumentException("商品が見つかりません: " + command.productId()));
        Inventory inventory = inventoryRepository.findByProductId(command.productId())
                .orElseGet(() -> {
                    Inventory newInventory = new Inventory();
                    newInventory.setProduct(product);
                    return newInventory;
                });
        inventory.setQuantity(command.quantity());
        return inventoryRepository.save(inventory);
    }

    public Void execute(DecreaseStockCommand command) {
        if (command.amount() < 0) {
            throw new ValidationException("amount", "減算数量は0以上である必要があります: " + command.amount());
        }
        try {
            Inventory inventory = inventoryRepository.findByProductIdForUpdate(command.productId())
                    .orElseThrow(() -> new IllegalStateException("在庫が見つかりません: " + command.productId()));
            inventory.decreaseStock(command.amount());
            inventoryRepository.save(inventory);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new OptimisticLockException("在庫の更新に失敗しました。他の操作と同時に更新されたため、再試行してください。", e);
        }
        return null;
    }

    public Void execute(IncreaseStockCommand command) {
        if (command.amount() < 0) {
            throw new ValidationException("amount", "増加分は0以上である必要があります: " + command.amount());
        }
        try {
            Inventory inventory = inventoryRepository.findByProductIdForUpdate(command.productId())
                    .orElseThrow(() -> new IllegalStateException("在庫が見つかりません: " + command.productId()));
            inventory.increaseStock(command.amount());
            inventoryRepository.save(inventory);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new OptimisticLockException("在庫の更新に失敗しました。他の操作と同時に更新されたため、再試行してください。", e);
        }
        return null;
    }

    public Void execute(ReserveStockCommand command) {
        if (command.amount() < 0) {
            throw new ValidationException("amount", "予約数量は0以上である必要があります: " + command.amount());
        }
        try {
            Inventory inventory = inventoryRepository.findByProductIdForUpdate(command.productId())
                    .orElseThrow(() -> new IllegalStateException("在庫が見つかりません: " + command.productId()));
            inventory.reserveStock(command.amount());
            inventoryRepository.save(inventory);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new OptimisticLockException("在庫の予約に失敗しました。他の操作と同時に更新されたため、再試行してください。", e);
        }
        return null;
    }

    public Void execute(ReleaseReservedStockCommand command) {
        if (command.amount() < 0) {
            throw new ValidationException("amount", "予約解除数量は0以上である必要があります: " + command.amount());
        }
        try {
            Inventory inventory = inventoryRepository.findByProductIdForUpdate(command.productId())
                    .orElseThrow(() -> new IllegalStateException("在庫が見つかりません: " + command.productId()));
            inventory.releaseReservedStock(command.amount());
            inventoryRepository.save(inventory);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new OptimisticLockException("在庫の予約解除に失敗しました。他の操作と同時に更新されたため、再試行してください。", e);
        }
        return null;
    }

    public Void execute(ConfirmReservedStockCommand command) {
        if (command.amount() < 0) {
            throw new ValidationException("amount", "予約確定数量は0以上である必要があります: " + command.amount());
        }
        try {
            Inventory inventory = inventoryRepository.findByProductIdForUpdate(command.productId())
                    .orElseThrow(() -> new IllegalStateException("在庫が見つかりません: " + command.productId()));
            inventory.confirmReservedStock(command.amount());
            inventoryRepository.save(inventory);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new OptimisticLockException("在庫の予約確定に失敗しました。他の操作と同時に更新されたため、再試行してください。", e);
        }
        return null;
    }
}
