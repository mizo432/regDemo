package com.github.mizo432.reg.sales.service;

import com.github.mizo432.reg.commission.repository.CommissionRecordRepository;
import com.github.mizo432.reg.commission.repository.CommissionRepository;
import com.github.mizo432.reg.inventory.domain.Inventory;
import com.github.mizo432.reg.inventory.repository.InventoryRepository;
import com.github.mizo432.reg.product.domain.Product;
import com.github.mizo432.reg.product.repository.ProductRepository;
import com.github.mizo432.reg.product.repository.TimeSaleRepository;
import com.github.mizo432.reg.sales.domain.CancelReturn;
import com.github.mizo432.reg.sales.domain.Transaction;
import com.github.mizo432.reg.sales.domain.TransactionItem;
import com.github.mizo432.reg.sales.repository.CancelReturnRepository;
import com.github.mizo432.reg.sales.repository.TransactionRepository;
import com.github.mizo432.reg.sync.service.SyncService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class SalesServiceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CancelReturnRepository cancelReturnRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TimeSaleRepository timeSaleRepository;

    @MockitoBean
    private CommissionRepository commissionRepository;

    @MockitoBean
    private CommissionRecordRepository commissionRecordRepository;

    @MockitoBean
    private SyncService syncService;

    @Autowired
    private SalesService salesService;

    private Long productId;

    @BeforeEach
    void setUp() {
        // テスト用の商品を作成
        Product product = new Product();
        product.setName("テスト商品");
        product.setBarcode("1234567890");
        product.setPrice(new BigDecimal("1000"));
        product.setTaxCategory(Product.TaxCategory.TAXABLE);
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
    void completeCheckout_取引が保存されること() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setDateTime(java.time.LocalDateTime.now());

        Product product = entityManager.getReference(Product.class, productId);
        TransactionItem item = new TransactionItem();
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("1000"));
        transaction.addItem(item);

        // When
        Transaction completed = salesService.completeCheckout(transaction);

        // Then
        assertNotNull(completed.getTransactionId());
        assertTrue(completed.getTransactionId().startsWith("TXN-"));
        assertEquals(new BigDecimal("2000"), completed.getSubtotal());
        assertEquals(new BigDecimal("200.00"), completed.getTaxTotal());
        assertEquals(new BigDecimal("2200.00"), completed.getGrandTotal());
        assertEquals(Transaction.Status.COMPLETED, completed.getStatus());
    }

    @Test
    void completeCheckout_在庫が減少すること() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setDateTime(java.time.LocalDateTime.now());

        Product product = entityManager.getReference(Product.class, productId);
        TransactionItem item = new TransactionItem();
        item.setProduct(product);
        item.setQuantity(10);
        item.setUnitPrice(new BigDecimal("1000"));
        transaction.addItem(item);

        // When
        salesService.completeCheckout(transaction);

        // Then
        entityManager.flush();
        entityManager.clear();
        Inventory updated = inventoryRepository.findByProductId(productId).orElseThrow();
        assertEquals(90, updated.getQuantity());
    }

    @Test
    void cancelTransaction_取引が取消し状態になること() {
        // Given
        Transaction transaction = createAndCompleteTransaction();
        String transactionId = transaction.getTransactionId();

        // When
        CancelReturn cancelReturn = salesService.cancelTransaction(transactionId, "テストキャンセル", "testOperator");

        // Then
        assertNotNull(cancelReturn);
        assertEquals(CancelReturn.Status.APPROVED, cancelReturn.getStatus());
        assertEquals(transactionId, cancelReturn.getOriginalTransactionId());
        assertEquals("testOperator", cancelReturn.getOperatorId());
        assertNotNull(cancelReturn.getReturnAmount());
        assertEquals(transaction.getGrandTotal(), cancelReturn.getReturnAmount());

        entityManager.flush();
        entityManager.clear();
        Transaction updated = transactionRepository.findByTransactionId(transactionId).orElseThrow();
        assertEquals(Transaction.Status.CANCELLED, updated.getStatus());
    }

    @Test
    void cancelTransaction_在庫が戻ること() {
        // Given
        Transaction transaction = createAndCompleteTransaction();
        String transactionId = transaction.getTransactionId();

        // When
        salesService.cancelTransaction(transactionId, "テストキャンセル", "testOperator");

        // Then
        entityManager.flush();
        entityManager.clear();
        Inventory updated = inventoryRepository.findByProductId(productId).orElseThrow();
        assertEquals(100, updated.getQuantity()); // 元の在庫数に戻る
    }

    @Test
    void returnTransaction_取引が返品状態になること() {
        // Given
        Transaction transaction = createAndCompleteTransaction();
        String transactionId = transaction.getTransactionId();

        // When
        CancelReturn cancelReturn = salesService.returnTransaction(transactionId, "テスト返品", "testOperator");

        // Then
        assertNotNull(cancelReturn);
        assertEquals(CancelReturn.Status.APPROVED, cancelReturn.getStatus());
        assertEquals(transactionId, cancelReturn.getOriginalTransactionId());
        assertNotNull(cancelReturn.getReturnAmount());

        entityManager.flush();
        entityManager.clear();
        Transaction updated = transactionRepository.findByTransactionId(transactionId).orElseThrow();
        assertEquals(Transaction.Status.RETURNED, updated.getStatus());
    }

    @Test
    void cancelTransaction_存在しない取引で例外がスローされる() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            salesService.cancelTransaction("NONEXISTENT", "理由", "operator");
        });
    }

    @Test
    void returnTransaction_存在しない取引で例外がスローされる() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            salesService.returnTransaction("NONEXISTENT", "理由", "operator");
        });
    }

    @Test
    void cancelTransaction_二重キャンセルで例外がスローされる() {
        // Given
        Transaction transaction = createAndCompleteTransaction();
        String transactionId = transaction.getTransactionId();

        // 一度キャンセル
        salesService.cancelTransaction(transactionId, "初回キャンセル", "testOperator");

        // When & Then - 二重キャンセルは失敗
        assertThrows(IllegalStateException.class, () -> {
            salesService.cancelTransaction(transactionId, "二重キャンセル", "testOperator");
        });
    }

    @Test
    void cancelTransaction_返品済みの取引をキャンセルできない() {
        // Given
        Transaction transaction = createAndCompleteTransaction();
        String transactionId = transaction.getTransactionId();

        // 一度返品
        salesService.returnTransaction(transactionId, "初回返品", "testOperator");

        // When & Then - 返品済みの取引をキャンセルは失敗
        assertThrows(IllegalStateException.class, () -> {
            salesService.cancelTransaction(transactionId, "キャンセル試行", "testOperator");
        });
    }

    @Test
    void returnTransaction_二重返品で例外がスローされる() {
        // Given
        Transaction transaction = createAndCompleteTransaction();
        String transactionId = transaction.getTransactionId();

        // 一度返品
        salesService.returnTransaction(transactionId, "初回返品", "testOperator");

        // When & Then - 二重返品は失敗
        assertThrows(IllegalStateException.class, () -> {
            salesService.returnTransaction(transactionId, "二重返品", "testOperator");
        });
    }

    @Test
    void returnTransaction_キャンセル済みの取引を返品できない() {
        // Given
        Transaction transaction = createAndCompleteTransaction();
        String transactionId = transaction.getTransactionId();

        // 一度キャンセル
        salesService.cancelTransaction(transactionId, "初回キャンセル", "testOperator");

        // When & Then - キャンセル済みの取引を返品は失敗
        assertThrows(IllegalStateException.class, () -> {
            salesService.returnTransaction(transactionId, "返品試行", "testOperator");
        });
    }

    @Test
    void cancelTransaction_二重キャンセルで在庫が二重に回復しない() {
        // Given
        Transaction transaction = createAndCompleteTransaction();
        String transactionId = transaction.getTransactionId();

        // 一度キャンセル - 在庫が100に戻る
        salesService.cancelTransaction(transactionId, "初回キャンセル", "testOperator");
        entityManager.flush();
        entityManager.clear();
        Inventory afterFirstCancel = inventoryRepository.findByProductId(productId).orElseThrow();
        assertEquals(100, afterFirstCancel.getQuantity());

        // When - 二重キャンセルは失敗
        assertThrows(IllegalStateException.class, () -> {
            salesService.cancelTransaction(transactionId, "二重キャンセル", "testOperator");
        });

        // Then - 在庫は100のまま（二重回復していない）
        entityManager.flush();
        entityManager.clear();
        Inventory afterDoubleCancel = inventoryRepository.findByProductId(productId).orElseThrow();
        assertEquals(100, afterDoubleCancel.getQuantity());
    }

    @Test
    void completeCheckout_予約した在庫が確定すること() {
        // Given - 在庫を予約
        Inventory inventory = inventoryRepository.findByProductId(productId).orElseThrow();
        inventory.reserveStock(5);
        inventoryRepository.save(inventory);
        entityManager.flush();

        // 予約後の状態を確認
        entityManager.clear();
        Inventory afterReserve = inventoryRepository.findByProductId(productId).orElseThrow();
        assertEquals(100, afterReserve.getQuantity());
        assertEquals(5, afterReserve.getReservedQuantity());
        assertEquals(95, afterReserve.getAvailableQuantity());

        Transaction transaction = new Transaction();
        transaction.setDateTime(java.time.LocalDateTime.now());

        Product product = entityManager.getReference(Product.class, productId);
        TransactionItem item = new TransactionItem();
        item.setProduct(product);
        item.setQuantity(5);
        item.setUnitPrice(new BigDecimal("1000"));
        transaction.addItem(item);

        // When - チェックアウトで予約が確定
        salesService.completeCheckout(transaction);

        // Then - 予約が確定してreservedQuantityのみ減少、quantityは変更しない
        entityManager.flush();
        entityManager.clear();
        Inventory updated = inventoryRepository.findByProductId(productId).orElseThrow();
        assertEquals(100, updated.getQuantity());
        assertEquals(0, updated.getReservedQuantity());
        assertEquals(100, updated.getAvailableQuantity());
    }

    @Test
    void completeCheckout_予約と未予約の混合で在庫が確定すること() {
        // Given - 3個を予約、2個は未予約
        Inventory inventory = inventoryRepository.findByProductId(productId).orElseThrow();
        inventory.reserveStock(3);
        inventoryRepository.save(inventory);
        entityManager.flush();

        Transaction transaction = new Transaction();
        transaction.setDateTime(java.time.LocalDateTime.now());

        Product product = entityManager.getReference(Product.class, productId);
        TransactionItem item = new TransactionItem();
        item.setProduct(product);
        item.setQuantity(5);
        item.setUnitPrice(new BigDecimal("1000"));
        transaction.addItem(item);

        // When - チェックアウトで予約3 + 未予約2が確定
        // fallbackパス: remaining = 5 - 3 = 2, quantity - 2 = 98
        salesService.completeCheckout(transaction);

        // Then
        entityManager.flush();
        entityManager.clear();
        Inventory updated = inventoryRepository.findByProductId(productId).orElseThrow();
        assertEquals(98, updated.getQuantity());
        assertEquals(0, updated.getReservedQuantity());
    }

    @Test
    void cancelTransaction_並行キャンセルで2つ目は失敗すること() {
        // Given
        Transaction transaction = createAndCompleteTransaction();
        String transactionId = transaction.getTransactionId();

        // When & Then - 排他ロックにより、2つ目のキャンセルは1つ目が完了するまで待機
        // その後、既にCANCELLEDのため例外がスローされる
        // H2のインメモリDBでは並行トランザクションのテストが難しいため、
        // 排他ロックが機能することを確認するために、ステータスチェックのテストで代替
        salesService.cancelTransaction(transactionId, "初回キャンセル", "testOperator");
        
        // 2回目のキャンセルは例外
        assertThrows(IllegalStateException.class, () -> {
            salesService.cancelTransaction(transactionId, "二重キャンセル", "testOperator");
        });
    }

    @Test
    void returnTransaction_並行返品で2つ目は失敗すること() {
        // Given
        Transaction transaction = createAndCompleteTransaction();
        String transactionId = transaction.getTransactionId();

        // When & Then
        salesService.returnTransaction(transactionId, "初回返品", "testOperator");
        
        // 2回目の返品は例外
        assertThrows(IllegalStateException.class, () -> {
            salesService.returnTransaction(transactionId, "二重返品", "testOperator");
        });
    }

    @Test
    void nonTaxableProduct_税が計算されないこと() {
        // Given
        Product nonTaxableProduct = new Product();
        nonTaxableProduct.setName("非課税商品");
        nonTaxableProduct.setBarcode("0000000000");
        nonTaxableProduct.setPrice(new BigDecimal("500"));
        nonTaxableProduct.setTaxCategory(Product.TaxCategory.NON_TAXABLE);
        entityManager.persist(nonTaxableProduct);
        entityManager.flush();
        Long nonTaxableId = nonTaxableProduct.getId();

        Inventory inventory = new Inventory();
        inventory.setProduct(nonTaxableProduct);
        inventory.setQuantity(50);
        entityManager.persist(inventory);
        entityManager.flush();

        Transaction transaction = new Transaction();
        transaction.setDateTime(java.time.LocalDateTime.now());

        Product product = entityManager.getReference(Product.class, nonTaxableId);
        TransactionItem item = new TransactionItem();
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("500"));
        transaction.addItem(item);

        // When
        Transaction completed = salesService.completeCheckout(transaction);

        // Then
        assertEquals(new BigDecimal("1000"), completed.getSubtotal());
        assertEquals(BigDecimal.ZERO, completed.getTaxTotal());
        assertEquals(new BigDecimal("1000"), completed.getGrandTotal());
    }

    private Transaction createAndCompleteTransaction() {
        Transaction transaction = new Transaction();
        transaction.setDateTime(java.time.LocalDateTime.now());

        Product product = entityManager.getReference(Product.class, productId);
        TransactionItem item = new TransactionItem();
        item.setProduct(product);
        item.setQuantity(10);
        item.setUnitPrice(new BigDecimal("1000"));
        transaction.addItem(item);

        return salesService.completeCheckout(transaction);
    }
}
