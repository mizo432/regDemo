package com.github.mizo432.reg.inventory.cqrs.handler;

import com.github.mizo432.reg.inventory.cqrs.query.GetAllInventoryQuery;
import com.github.mizo432.reg.inventory.cqrs.query.GetInventoryByProductIdQuery;
import com.github.mizo432.reg.inventory.cqrs.query.HasStockQuery;
import com.github.mizo432.reg.inventory.domain.Inventory;
import com.github.mizo432.reg.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryQueryHandlerTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryQueryHandler handler;

    @Test
    void handle_GetAllInventoryQuery_全在庫が返されること() {
        // Given
        List<Inventory> inventories = List.of(createInventory(1L), createInventory(2L));
        when(inventoryRepository.findAll()).thenReturn(inventories);

        // When
        List<Inventory> result = handler.handle(new GetAllInventoryQuery());

        // Then
        assertEquals(2, result.size());
        verify(inventoryRepository).findAll();
    }

    @Test
    void handle_GetInventoryByProductIdQuery_在庫が返されること() {
        // Given
        Inventory inventory = createInventory(1L);
        inventory.setQuantity(100);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

        // When
        Optional<Inventory> result = handler.handle(new GetInventoryByProductIdQuery(1L));

        // Then
        assertTrue(result.isPresent());
        assertEquals(100, result.get().getQuantity());
    }

    @Test
    void handle_GetInventoryByProductIdQuery_在庫が存在しない場合はemptyを返すこと() {
        // Given
        when(inventoryRepository.findByProductId(999L)).thenReturn(Optional.empty());

        // When
        Optional<Inventory> result = handler.handle(new GetInventoryByProductIdQuery(999L));

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void handle_HasStockQuery_在庫が十分な場合はtrueを返すこと() {
        // Given
        Inventory inventory = createInventory(1L);
        inventory.setQuantity(100);
        inventory.setReservedQuantity(10);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

        // When
        Boolean result = handler.handle(new HasStockQuery(1L, 50));

        // Then
        assertTrue(result);
    }

    @Test
    void handle_HasStockQuery_在庫が不足する場合はfalseを返すこと() {
        // Given
        Inventory inventory = createInventory(1L);
        inventory.setQuantity(100);
        inventory.setReservedQuantity(80);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

        // When
        Boolean result = handler.handle(new HasStockQuery(1L, 50));

        // Then
        assertFalse(result);
    }

    @Test
    void handle_HasStockQuery_在庫が存在しない場合はfalseを返すこと() {
        // Given
        when(inventoryRepository.findByProductId(999L)).thenReturn(Optional.empty());

        // When
        Boolean result = handler.handle(new HasStockQuery(999L, 10));

        // Then
        assertFalse(result);
    }

    @Test
    void handle_HasStockQuery_予約分を引いてちょうど一致する場合はtrueを返すこと() {
        // Given
        Inventory inventory = createInventory(1L);
        inventory.setQuantity(100);
        inventory.setReservedQuantity(50);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

        // When
        Boolean result = handler.handle(new HasStockQuery(1L, 50));

        // Then
        assertTrue(result);
    }

    private Inventory createInventory(Long productId) {
        Inventory inventory = new Inventory();
        inventory.setId(productId);
        inventory.setQuantity(0);
        inventory.setReservedQuantity(0);
        return inventory;
    }
}
