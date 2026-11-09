package com.github.mizo432.reg.inventory.cqrs.handler;

import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.inventory.cqrs.command.*;
import com.github.mizo432.reg.inventory.domain.Inventory;
import com.github.mizo432.reg.inventory.repository.InventoryRepository;
import com.github.mizo432.reg.product.domain.Product;
import com.github.mizo432.reg.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryCommandHandlerTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private InventoryCommandHandler handler;

    @Test
    void execute_UpdateInventoryCommand_既存在庫が更新されること() {
        // Given
        Product product = createProduct(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Inventory existingInventory = new Inventory();
        existingInventory.setId(1L);
        existingInventory.setQuantity(50);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(existingInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(existingInventory);

        UpdateInventoryCommand command = new UpdateInventoryCommand(1L, 100);

        // When
        Inventory result = handler.execute(command);

        // Then
        assertEquals(100, result.getQuantity());
        verify(inventoryRepository).save(existingInventory);
    }

    @Test
    void execute_UpdateInventoryCommand_在庫が存在しない場合は新規作成されること() {
        // Given
        Product product = createProduct(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.empty());

        Inventory savedInventory = new Inventory();
        savedInventory.setId(1L);
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(savedInventory);

        UpdateInventoryCommand command = new UpdateInventoryCommand(1L, 50);

        // When
        handler.execute(command);

        // Then
        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());
        Inventory captured = captor.getValue();
        assertEquals(50, captured.getQuantity());
        assertEquals(1L, captured.getProduct().getId());
    }

    @Test
    void execute_UpdateInventoryCommand_商品が存在しない場合は例外がスローされる() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateInventoryCommand command = new UpdateInventoryCommand(999L, 50);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_DecreaseStockCommand_在庫が減少すること() {
        // Given
        Inventory inventory = createInventory(1L, 100, 0);
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        DecreaseStockCommand command = new DecreaseStockCommand(1L, 10);

        // When
        handler.execute(command);

        // Then
        assertEquals(90, inventory.getQuantity());
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void execute_DecreaseStockCommand_在庫不足で例外がスローされる() {
        // Given
        Inventory inventory = createInventory(1L, 10, 0);
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));

        DecreaseStockCommand command = new DecreaseStockCommand(1L, 20);

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_IncreaseStockCommand_在庫が増加すること() {
        // Given
        Inventory inventory = createInventory(1L, 100, 0);
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        IncreaseStockCommand command = new IncreaseStockCommand(1L, 50);

        // When
        handler.execute(command);

        // Then
        assertEquals(150, inventory.getQuantity());
    }

    @Test
    void execute_ReserveStockCommand_予約数量が増加すること() {
        // Given
        Inventory inventory = createInventory(1L, 100, 0);
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        ReserveStockCommand command = new ReserveStockCommand(1L, 10);

        // When
        handler.execute(command);

        // Then
        assertEquals(10, inventory.getReservedQuantity());
        assertEquals(90, inventory.getAvailableQuantity());
    }

    @Test
    void execute_ReleaseReservedStockCommand_予約数量が減少すること() {
        // Given
        Inventory inventory = createInventory(1L, 100, 20);
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        ReleaseReservedStockCommand command = new ReleaseReservedStockCommand(1L, 10);

        // When
        handler.execute(command);

        // Then
        assertEquals(10, inventory.getReservedQuantity());
    }

    @Test
    void execute_ConfirmReservedStockCommand_予約が確定すること() {
        // Given
        Inventory inventory = createInventory(1L, 100, 20);
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        ConfirmReservedStockCommand command = new ConfirmReservedStockCommand(1L, 10);

        // When
        handler.execute(command);

        // Then - reservedQuantityのみ減算、quantityは変更しない
        assertEquals(100, inventory.getQuantity());
        assertEquals(10, inventory.getReservedQuantity());
    }

    @Test
    void execute_DecreaseStockCommand_在庫が存在しない場合は例外がスローされる() {
        // Given
        when(inventoryRepository.findByProductIdForUpdate(999L)).thenReturn(Optional.empty());

        DecreaseStockCommand command = new DecreaseStockCommand(999L, 10);

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            handler.execute(command);
        });
    }

    // === 敵対的検証: 負の値・境界値（修正済み - 検証が追加された） ===

    @Test
    void execute_IncreaseStockCommand_負の値でValidationExceptionがスローされる() {
        // Given
        IncreaseStockCommand command = new IncreaseStockCommand(1L, -50);

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_DecreaseStockCommand_負の値でValidationExceptionがスローされる() {
        // Given
        DecreaseStockCommand command = new DecreaseStockCommand(1L, -50);

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_ReserveStockCommand_負の値でValidationExceptionがスローされる() {
        // Given
        ReserveStockCommand command = new ReserveStockCommand(1L, -5);

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_ReleaseReservedStockCommand_負の値でValidationExceptionがスローされる() {
        // Given
        ReleaseReservedStockCommand command = new ReleaseReservedStockCommand(1L, -5);

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_ConfirmReservedStockCommand_負の値でValidationExceptionがスローされる() {
        // Given
        ConfirmReservedStockCommand command = new ConfirmReservedStockCommand(1L, -5);

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_UpdateInventoryCommand_負のquantityでValidationExceptionがスローされる() {
        // Given
        UpdateInventoryCommand command = new UpdateInventoryCommand(1L, -100);

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_UpdateInventoryCommand_0のquantityが設定されること() {
        // 0は有効なケースがあるため許容
        // Given
        Product product = createProduct(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Inventory existingInventory = new Inventory();
        existingInventory.setId(1L);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(existingInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(existingInventory);

        UpdateInventoryCommand command = new UpdateInventoryCommand(1L, 0);

        // When
        handler.execute(command);

        // Then
        assertEquals(0, existingInventory.getQuantity());
    }

    @Test
    void execute_IncreaseStockCommand_0で変更なし() {
        // Given
        Inventory inventory = createInventory(1L, 100, 0);
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        IncreaseStockCommand command = new IncreaseStockCommand(1L, 0);

        // When
        handler.execute(command);

        // Then
        assertEquals(100, inventory.getQuantity());
    }

    @Test
    void execute_DecreaseStockCommand_0で変更なし() {
        // Given
        Inventory inventory = createInventory(1L, 100, 0);
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        DecreaseStockCommand command = new DecreaseStockCommand(1L, 0);

        // When
        handler.execute(command);

        // Then
        assertEquals(100, inventory.getQuantity());
    }

    @Test
    void execute_ReserveStockCommand_0で変更なし() {
        // Given
        Inventory inventory = createInventory(1L, 100, 0);
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        ReserveStockCommand command = new ReserveStockCommand(1L, 0);

        // When
        handler.execute(command);

        // Then
        assertEquals(0, inventory.getReservedQuantity());
    }

    @Test
    void execute_IncreaseStockCommand_Integer_MAX_VALUEでオーバーフローすること() {
        // intのオーバーフローはドメインモデルでは防げない（ハンドラー層で検証）
        // Given
        Inventory inventory = createInventory(1L, Integer.MAX_VALUE - 10, 0);
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        IncreaseStockCommand command = new IncreaseStockCommand(1L, 100);

        // When
        handler.execute(command);

        // Then - オーバーフローで負の値になる
        assertTrue(inventory.getQuantity() < 0, "オーバーフローで負の値になるべき");
    }

    private Product createProduct(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setName("テスト商品");
        product.setBarcode("1234567890");
        product.setPrice(new java.math.BigDecimal("1000"));
        product.setTaxCategory(Product.TaxCategory.TAXABLE);
        return product;
    }

    private Inventory createInventory(Long productId, int quantity, int reservedQuantity) {
        Inventory inventory = new Inventory();
        inventory.setId(productId);
        inventory.setProduct(createProduct(productId));
        inventory.setQuantity(quantity);
        inventory.setReservedQuantity(reservedQuantity);
        return inventory;
    }
}
