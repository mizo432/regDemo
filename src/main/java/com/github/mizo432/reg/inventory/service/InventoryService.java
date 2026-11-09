package com.github.mizo432.reg.inventory.service;

import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.inventory.cqrs.command.ConfirmReservedStockCommand;
import com.github.mizo432.reg.inventory.cqrs.command.DecreaseStockCommand;
import com.github.mizo432.reg.inventory.cqrs.command.IncreaseStockCommand;
import com.github.mizo432.reg.inventory.cqrs.command.ReleaseReservedStockCommand;
import com.github.mizo432.reg.inventory.cqrs.command.ReserveStockCommand;
import com.github.mizo432.reg.inventory.cqrs.command.UpdateInventoryCommand;
import com.github.mizo432.reg.inventory.cqrs.handler.InventoryCommandHandler;
import com.github.mizo432.reg.inventory.cqrs.handler.InventoryQueryHandler;
import com.github.mizo432.reg.inventory.cqrs.query.GetAllInventoryQuery;
import com.github.mizo432.reg.inventory.cqrs.query.GetInventoryByProductIdQuery;
import com.github.mizo432.reg.inventory.cqrs.query.HasStockQuery;
import com.github.mizo432.reg.inventory.domain.Inventory;
import com.github.mizo432.reg.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 在庫サービスのファサード。
 * CQRSパターンを採用し、コマンド操作はInventoryCommandHandler、クエリ操作はInventoryQueryHandlerに委譲します。
 * 既存のAPIを維持するため、このファサードクラスを介してアクセスします。
 */
@Service
public class InventoryService {

    private final InventoryCommandHandler commandHandler;
    private final InventoryQueryHandler queryHandler;
    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryCommandHandler commandHandler, 
                           InventoryQueryHandler queryHandler,
                           InventoryRepository inventoryRepository) {
        this.commandHandler = commandHandler;
        this.queryHandler = queryHandler;
        this.inventoryRepository = inventoryRepository;
    }

    public List<Inventory> findAll() {
        return queryHandler.handle(new GetAllInventoryQuery());
    }

    public Optional<Inventory> findByProductId(Long productId) {
        if (productId == null) {
            throw new ValidationException("productId", "商品IDはnullであってはいけません");
        }
        return queryHandler.handle(new GetInventoryByProductIdQuery(productId));
    }

    public Optional<Inventory> findById(Long id) {
        if (id == null) {
            throw new ValidationException("id", "IDはnullであってはいけません");
        }
        return inventoryRepository.findById(id);
    }

    public Inventory save(Inventory inventory) {
        if (inventory == null) {
            throw new ValidationException("inventory", "在庫はnullであってはいけません");
        }
        if (inventory.getProduct() == null) {
            throw new ValidationException("product", "商品はnullであってはいけません");
        }
        if (inventory.getQuantity() < 0) {
            throw new ValidationException("quantity", "在庫数量は0以上である必要があります: " + inventory.getQuantity());
        }
        return inventoryRepository.save(inventory);
    }

    public Inventory createOrUpdateInventory(Long productId, int quantity) {
        if (productId == null) {
            throw new ValidationException("productId", "商品IDはnullであってはいけません");
        }
        if (quantity < 0) {
            throw new ValidationException("quantity", "数量は0以上である必要があります: " + quantity);
        }
        return commandHandler.execute(new UpdateInventoryCommand(productId, quantity));
    }

    public void decreaseStock(Long productId, int amount) {
        if (productId == null) {
            throw new ValidationException("productId", "商品IDはnullであってはいけません");
        }
        if (amount < 0) {
            throw new ValidationException("amount", "数量は0以上である必要があります: " + amount);
        }
        commandHandler.execute(new DecreaseStockCommand(productId, amount));
    }

    public void increaseStock(Long productId, int amount) {
        if (productId == null) {
            throw new ValidationException("productId", "商品IDはnullであってはいけません");
        }
        if (amount < 0) {
            throw new ValidationException("amount", "数量は0以上である必要があります: " + amount);
        }
        commandHandler.execute(new IncreaseStockCommand(productId, amount));
    }

    public void reserveStock(Long productId, int amount) {
        if (productId == null) {
            throw new ValidationException("productId", "商品IDはnullであってはいけません");
        }
        if (amount < 0) {
            throw new ValidationException("amount", "数量は0以上である必要があります: " + amount);
        }
        commandHandler.execute(new ReserveStockCommand(productId, amount));
    }

    public void releaseReservedStock(Long productId, int amount) {
        if (productId == null) {
            throw new ValidationException("productId", "商品IDはnullであってはいけません");
        }
        if (amount < 0) {
            throw new ValidationException("amount", "数量は0以上である必要があります: " + amount);
        }
        commandHandler.execute(new ReleaseReservedStockCommand(productId, amount));
    }

    public void confirmReservedStock(Long productId, int amount) {
        if (productId == null) {
            throw new ValidationException("productId", "商品IDはnullであってはいけません");
        }
        if (amount < 0) {
            throw new ValidationException("amount", "数量は0以上である必要があります: " + amount);
        }
        commandHandler.execute(new ConfirmReservedStockCommand(productId, amount));
    }

    public boolean hasStock(Long productId, int amount) {
        if (productId == null) {
            throw new ValidationException("productId", "商品IDはnullであってはいけません");
        }
        if (amount < 0) {
            throw new ValidationException("amount", "数量は0以上である必要があります: " + amount);
        }
        return queryHandler.handle(new HasStockQuery(productId, amount));
    }
}
