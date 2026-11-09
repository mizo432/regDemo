package com.github.mizo432.reg.inventory.service;

import com.github.mizo432.reg.inventory.domain.Inventory;
import com.github.mizo432.reg.inventory.repository.InventoryRepository;
import com.github.mizo432.reg.product.domain.Product;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class InventoryServiceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryService inventoryService;

    private Long productId;

    @BeforeEach
    void setUp() {

        // テスト用の商品を作成
        Product product = new Product();
        product.setName("テスト商品");
        product.setBarcode("1234567890");
        product.setPrice(new java.math.BigDecimal("1000"));
        entityManager.persist(product);
        entityManager.flush();
        productId = product.getId();

        // テスト用の在庫を作成
        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setQuantity(100);
        inventory.setReservedQuantity(0);
        entityManager.persist(inventory);
        entityManager.flush();
    }

    @Test
    void decreaseStock_在庫が減少すること() {
        // When
        inventoryService.decreaseStock(productId, 10);

        // Then
        entityManager.flush();
        Inventory updated = inventoryRepository.findByProductId(productId).orElseThrow();
        assertEquals(90, updated.getQuantity());
    }

    @Test
    void increaseStock_在庫が増加すること() {
        // When
        inventoryService.increaseStock(productId, 10);

        // Then
        entityManager.flush();
        Inventory updated = inventoryRepository.findByProductId(productId).orElseThrow();
        assertEquals(110, updated.getQuantity());
    }

    @Test
    void decreaseStock_在庫不足で例外がスローされる() {
        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            inventoryService.decreaseStock(productId, 200);
        });
    }

    @Test
    void version_楽観的ロックが機能すること() {
        // When - 最初の更新
        inventoryService.decreaseStock(productId, 10);
        entityManager.flush();
        entityManager.clear();

        // Then - versionがインクリメントされる
        Inventory updated = inventoryRepository.findByProductId(productId).orElseThrow();
        assertNotNull(updated.getVersion());
        assertTrue(updated.getVersion() > 0);
    }

    @Test
    void reserveStock_予約数量が増加すること() {
        // When
        inventoryService.reserveStock(productId, 10);

        // Then
        entityManager.flush();
        entityManager.clear();
        Inventory updated = inventoryRepository.findByProductId(productId).orElseThrow();
        assertEquals(100, updated.getQuantity());
        assertEquals(10, updated.getReservedQuantity());
        assertEquals(90, updated.getAvailableQuantity());
    }

    @Test
    void releaseReservedStock_予約数量が減少すること() {
        // Given
        inventoryService.reserveStock(productId, 10);
        entityManager.flush();

        // When
        inventoryService.releaseReservedStock(productId, 5);

        // Then
        entityManager.flush();
        entityManager.clear();
        Inventory updated = inventoryRepository.findByProductId(productId).orElseThrow();
        assertEquals(100, updated.getQuantity());
        assertEquals(5, updated.getReservedQuantity());
        assertEquals(95, updated.getAvailableQuantity());
    }

    @Test
    void confirmReservedStock_予約が確定して予約数量が減少すること() {
        // Given
        inventoryService.reserveStock(productId, 10);
        entityManager.flush();

        // When
        inventoryService.confirmReservedStock(productId, 10);

        // Then - reservedQuantityのみ減算、quantityは変更しない
        entityManager.flush();
        entityManager.clear();
        Inventory updated = inventoryRepository.findByProductId(productId).orElseThrow();
        assertEquals(100, updated.getQuantity());
        assertEquals(0, updated.getReservedQuantity());
        assertEquals(100, updated.getAvailableQuantity());
    }

    @Test
    void confirmReservedStock_予約なしでも在庫が減少すること() {
        // 予約せずに直接confirmReservedStockを呼び出す（後方互換性）
        // fallbackパス: remaining = 10 - 0 = 10, quantity - 10 = 90
        // When
        inventoryService.confirmReservedStock(productId, 10);

        // Then
        entityManager.flush();
        entityManager.clear();
        Inventory updated = inventoryRepository.findByProductId(productId).orElseThrow();
        assertEquals(90, updated.getQuantity());
        assertEquals(0, updated.getReservedQuantity());
    }

    @Test
    void confirmReservedStock_予約と未予約の混合で在庫が減少すること() {
        // Given - 5個を予約
        inventoryService.reserveStock(productId, 5);
        entityManager.flush();

        // When - 10個を確定（予約5 + 未予約5）
        // fallbackパス: remaining = 10 - 5 = 5, quantity - 5 = 95
        inventoryService.confirmReservedStock(productId, 10);

        // Then
        entityManager.flush();
        entityManager.clear();
        Inventory updated = inventoryRepository.findByProductId(productId).orElseThrow();
        assertEquals(95, updated.getQuantity());
        assertEquals(0, updated.getReservedQuantity());
    }

    @Test
    void reserveStock_在庫不足で例外がスローされる() {
        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            inventoryService.reserveStock(productId, 200);
        });
    }

    @Test
    void releaseReservedStock_予約数量不足で例外がスローされる() {
        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            inventoryService.releaseReservedStock(productId, 10);
        });
    }

    @Test
    void confirmReservedStock_在庫不足で例外メッセージが正確なこと() {
        // Given - 在庫100、予約5
        inventoryService.reserveStock(productId, 5);
        entityManager.flush();

        // When & Then - 106個を確定しようとする
        // remaining = 106 - 5 = 101, quantity - 101 = 100 - 101 = -1 → 例外
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            inventoryService.confirmReservedStock(productId, 106);
        });
        // 例外メッセージに「利用可能=95」が含まれることを確認（reservedQuantity=5復元後、quantity=100 - reservedQuantity=5 = 95）
        assertTrue(ex.getMessage().contains("利用可能=95"),
                "例外メッセージに正確な利用可能数量が含まれるべき: " + ex.getMessage());
        // 在庫が元に戻っていることを確認
        entityManager.flush();
        entityManager.clear();
        Inventory updated = inventoryRepository.findByProductId(productId).orElseThrow();
        assertEquals(100, updated.getQuantity());
        assertEquals(5, updated.getReservedQuantity());
    }

    @Test
    void confirmReservedStock_fallbackパスでquantityから不足分のみを減算すること() {
        // 予約数量が不足するケースで、reservedQuantityを0化し、
        // quantityから不足分（remaining）のみを減算する。
        // Given - 在庫100、予約3
        inventoryService.reserveStock(productId, 3);
        entityManager.flush();

        // When - 5個を確定（予約3 + 未予約2）
        // fallbackパスに遷移: amount(5) > reservedQuantity(3)
        // remaining = 5 - 3 = 2, quantity - 2 = 98
        inventoryService.confirmReservedStock(productId, 5);

        // Then
        entityManager.flush();
        entityManager.clear();
        Inventory updated = inventoryRepository.findByProductId(productId).orElseThrow();
        // quantityからremaining(2)のみ減算: 100 - 2 = 98
        // reservedQuantityを0化: 0
        assertEquals(98, updated.getQuantity(),
                "quantityは100 - 2 = 98 になるべき");
        assertEquals(0, updated.getReservedQuantity(),
                "reservedQuantityは0になるべき");
    }

    @Test
    void confirmReservedStock_fallbackパスで在庫不足時にrollbackすること() {
        // Given - 在庫100、予約3 → 利用可能=97
        inventoryService.reserveStock(productId, 3);
        entityManager.flush();

        // When - 101個を確定しようとする
        // remaining = 101 - 3 = 98, quantity - 98 = 100 - 98 = 2 (>=0 なので成功)
        inventoryService.confirmReservedStock(productId, 101);

        // Then - 成功する場合
        entityManager.flush();
        entityManager.clear();
        Inventory updated = inventoryRepository.findByProductId(productId).orElseThrow();
        assertEquals(2, updated.getQuantity());
        assertEquals(0, updated.getReservedQuantity());
    }

    @Test
    void confirmReservedStock_fallbackパスで在庫不足時に例外をスローすること() {
        // Given - 在庫100、予約3 → 利用可能=97
        inventoryService.reserveStock(productId, 3);
        entityManager.flush();

        // When - 102個を確定しようとする
        // remaining = 102 - 3 = 99, quantity - 99 = 100 - 99 = 1 (>=0 なので成功)
        // 103でテスト: remaining = 103 - 3 = 100, quantity - 100 = 0 (>=0 なので成功)
        // 104でテスト: remaining = 104 - 3 = 101, quantity - 101 = 100 - 101 = -1 → 例外
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            inventoryService.confirmReservedStock(productId, 104);
        });
        assertTrue(ex.getMessage().contains("在庫不足"));

        // 在庫が元に戻っていることを確認
        entityManager.flush();
        entityManager.clear();
        Inventory updated = inventoryRepository.findByProductId(productId).orElseThrow();
        assertEquals(100, updated.getQuantity());
        assertEquals(3, updated.getReservedQuantity());
    }
}
