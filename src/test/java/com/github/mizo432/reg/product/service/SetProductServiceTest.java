package com.github.mizo432.reg.product.service;

import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.product.domain.Product;
import com.github.mizo432.reg.product.domain.SetProduct;
import com.github.mizo432.reg.product.domain.SetProductItem;
import com.github.mizo432.reg.product.repository.ProductRepository;
import com.github.mizo432.reg.product.repository.SetProductRepository;
import com.github.mizo432.reg.product.repository.TimeSaleRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class SetProductServiceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SetProductService setProductService;

    @Autowired
    private SetProductRepository setProductRepository;

    @Autowired
    private ProductRepository productRepository;

    private Long productId1;
    private Long productId2;

    @BeforeEach
    void setUp() {
        // テスト用の商品を作成
        Product product1 = new Product();
        product1.setName("商品A");
        product1.setBarcode("1111111111");
        product1.setPrice(new BigDecimal("500"));
        product1.setTaxCategory(Product.TaxCategory.TAXABLE);
        entityManager.persist(product1);

        Product product2 = new Product();
        product2.setName("商品B");
        product2.setBarcode("2222222222");
        product2.setPrice(new BigDecimal("300"));
        product2.setTaxCategory(Product.TaxCategory.TAXABLE);
        entityManager.persist(product2);

        entityManager.flush();
        productId1 = product1.getId();
        productId2 = product2.getId();
    }

    @Test
    void save_セット商品が保存されること() {
        // Given
        SetProduct setProduct = new SetProduct();
        setProduct.setSetBarcode("SET001");
        setProduct.setSetName("セット商品A");
        setProduct.setSetPrice(new BigDecimal("700"));

        Product product1 = entityManager.getReference(Product.class, productId1);
        SetProductItem item1 = new SetProductItem();
        item1.setProduct(product1);
        item1.setQuantity(1);
        setProduct.addItem(item1);

        Product product2 = entityManager.getReference(Product.class, productId2);
        SetProductItem item2 = new SetProductItem();
        item2.setProduct(product2);
        item2.setQuantity(2);
        setProduct.addItem(item2);

        // When
        SetProduct saved = setProductService.save(setProduct);

        // Then
        assertNotNull(saved.getId());
        assertEquals("SET001", saved.getSetBarcode());
        assertEquals("セット商品A", saved.getSetName());
        assertEquals(new BigDecimal("700"), saved.getSetPrice());
        assertEquals(2, saved.getItems().size());
    }

    @Test
    void findBySetBarcode_バーコードで検索できること() {
        // Given
        SetProduct setProduct = new SetProduct();
        setProduct.setSetBarcode("SET002");
        setProduct.setSetName("セット商品B");
        setProduct.setSetPrice(new BigDecimal("800"));
        entityManager.persist(setProduct);
        entityManager.flush();

        // When
        Optional<SetProduct> found = setProductService.findBySetBarcode("SET002");

        // Then
        assertTrue(found.isPresent());
        assertEquals("SET002", found.get().getSetBarcode());
    }

    @Test
    void expandByBarcode_内包商品が展開されること() {
        // Given
        SetProduct setProduct = new SetProduct();
        setProduct.setSetBarcode("SET003");
        setProduct.setSetName("セット商品C");
        setProduct.setSetPrice(new BigDecimal("600"));

        Product product1 = entityManager.getReference(Product.class, productId1);
        SetProductItem item1 = new SetProductItem();
        item1.setProduct(product1);
        item1.setQuantity(1);
        setProduct.addItem(item1);

        Product product2 = entityManager.getReference(Product.class, productId2);
        SetProductItem item2 = new SetProductItem();
        item2.setProduct(product2);
        item2.setQuantity(2);
        setProduct.addItem(item2);

        entityManager.persist(setProduct);
        entityManager.flush();

        // When
        Optional<SetProductExpansion> expansion = setProductService.expandByBarcode("SET003");

        // Then
        assertTrue(expansion.isPresent());
        assertEquals("セット商品C", expansion.get().setName());
        assertEquals(new BigDecimal("600"), expansion.get().setPrice());
        assertEquals(2, expansion.get().items().size());

        // 内包商品の確認
        List<SetProductExpansion.ExpandedItem> items = expansion.get().items();
        assertEquals("商品A", items.get(0).name());
        assertEquals(1, items.get(0).quantity());
        assertEquals("商品B", items.get(1).name());
        assertEquals(2, items.get(1).quantity());
    }

    @Test
    void expandByBarcode_セット商品価格が内包商品に比例配分されること() {
        // Given
        // 商品A: 500円 × 1 = 500円
        // 商品B: 300円 × 2 = 600円
        // 合計: 1100円
        // セット価格: 600円（割引）
        // 商品Aの配分合計: 600 × 500/1100 = 272.73円
        // 商品Bの配分合計: 600 - 272.73 = 327.27円（残差補正）

        SetProduct setProduct = new SetProduct();
        setProduct.setSetBarcode("SET_PRICE");
        setProduct.setSetName("価格テストセット");
        setProduct.setSetPrice(new BigDecimal("600"));

        Product product1 = entityManager.getReference(Product.class, productId1);
        SetProductItem item1 = new SetProductItem();
        item1.setProduct(product1);
        item1.setQuantity(1);
        setProduct.addItem(item1);

        Product product2 = entityManager.getReference(Product.class, productId2);
        SetProductItem item2 = new SetProductItem();
        item2.setProduct(product2);
        item2.setQuantity(2);
        setProduct.addItem(item2);

        entityManager.persist(setProduct);
        entityManager.flush();

        // When
        Optional<SetProductExpansion> expansion = setProductService.expandByBarcode("SET_PRICE");

        // Then
        assertTrue(expansion.isPresent());
        List<SetProductExpansion.ExpandedItem> items = expansion.get().items();

        // 配分された価格を確認
        // 商品A: 600 × 500/1100 = 272.73
        assertEquals(new BigDecimal("272.73"), items.get(0).unitPrice());
        // 商品B: 残差補正 (600 - 272.73) / 2 = 163.64 (327.27 / 2 = 163.635 → 163.64)
        assertEquals(new BigDecimal("163.64"), items.get(1).unitPrice());

        // 配分後の合計がセット商品価格に±0.01円の許容範囲内であることを確認
        // （四捨五入により完全一致しない場合がある）
        BigDecimal allocatedTotal = BigDecimal.ZERO;
        for (SetProductExpansion.ExpandedItem item : items) {
            allocatedTotal = allocatedTotal.add(
                item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()))
            );
        }
        // 272.73 × 1 + 163.64 × 2 = 272.73 + 327.28 = 600.01
        // 許容範囲: ±0.01円
        assertTrue(allocatedTotal.subtract(new BigDecimal("600")).abs()
                .compareTo(BigDecimal.valueOf(0.01)) <= 0,
            "配分後の合計(" + allocatedTotal + ")はセット商品価格(600)に±0.01円の範囲内であるべき");
    }

    @Test
    void expandByBarcode_セット価格が内包商品合計より高い場合も配分されること() {
        // Given
        // 商品A: 500円 × 1 = 500円
        // 商品B: 300円 × 1 = 300円
        // 合計: 800円
        // セット価格: 900円（内包商品合計より高い）

        SetProduct setProduct = new SetProduct();
        setProduct.setSetBarcode("SET_HIGH");
        setProduct.setSetName("高価格セット");
        setProduct.setSetPrice(new BigDecimal("900"));

        Product product1 = entityManager.getReference(Product.class, productId1);
        SetProductItem item1 = new SetProductItem();
        item1.setProduct(product1);
        item1.setQuantity(1);
        setProduct.addItem(item1);

        Product product2 = entityManager.getReference(Product.class, productId2);
        SetProductItem item2 = new SetProductItem();
        item2.setProduct(product2);
        item2.setQuantity(1);
        setProduct.addItem(item2);

        entityManager.persist(setProduct);
        entityManager.flush();

        // When
        Optional<SetProductExpansion> expansion = setProductService.expandByBarcode("SET_HIGH");

        // Then
        assertTrue(expansion.isPresent());
        List<SetProductExpansion.ExpandedItem> items = expansion.get().items();

        // 配分後の合計がセット商品価格に±0.01円の許容範囲内であることを確認
        BigDecimal allocatedTotal = BigDecimal.ZERO;
        for (SetProductExpansion.ExpandedItem item : items) {
            allocatedTotal = allocatedTotal.add(
                item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()))
            );
        }
        assertTrue(allocatedTotal.subtract(new BigDecimal("900")).abs()
                .compareTo(BigDecimal.valueOf(0.01)) <= 0,
            "配分後の合計(" + allocatedTotal + ")はセット商品価格(900)に±0.01円の範囲内であるべき");
    }

    @Test
    void expandByBarcode_INACTIVEなセット商品は展開されないこと() {
        // Given
        SetProduct setProduct = new SetProduct();
        setProduct.setSetBarcode("SET004");
        setProduct.setSetName("セット商品D");
        setProduct.setSetPrice(new BigDecimal("900"));
        setProduct.setStatus(SetProduct.Status.INACTIVE);
        entityManager.persist(setProduct);
        entityManager.flush();

        // When
        Optional<SetProductExpansion> expansion = setProductService.expandByBarcode("SET004");

        // Then
        assertTrue(expansion.isEmpty());
    }

    @Test
    void findActiveSetProducts_ACTIVEなセット商品のみ返ること() {
        // Given
        SetProduct activeSet = new SetProduct();
        activeSet.setSetBarcode("SET005");
        activeSet.setSetName("アクティブセット");
        activeSet.setSetPrice(new BigDecimal("1000"));
        activeSet.setStatus(SetProduct.Status.ACTIVE);
        entityManager.persist(activeSet);

        SetProduct inactiveSet = new SetProduct();
        inactiveSet.setSetBarcode("SET006");
        inactiveSet.setSetName("非アクティブセット");
        inactiveSet.setSetPrice(new BigDecimal("2000"));
        inactiveSet.setStatus(SetProduct.Status.INACTIVE);
        entityManager.persist(inactiveSet);

        entityManager.flush();

        // When
        List<SetProduct> activeSets = setProductService.findActiveSetProducts();

        // Then
        assertEquals(1, activeSets.size());
        assertEquals("SET005", activeSets.get(0).getSetBarcode());
    }

    @Test
    void deleteById_セット商品が削除されること() {
        // Given
        SetProduct setProduct = new SetProduct();
        setProduct.setSetBarcode("SET007");
        setProduct.setSetName("削除対象");
        setProduct.setSetPrice(new BigDecimal("500"));
        entityManager.persist(setProduct);
        Long id = setProduct.getId();
        entityManager.flush();

        // When
        setProductService.deleteById(id);

        // Then
        assertTrue(setProductRepository.findById(id).isEmpty());
    }

    @Test
    void expandByBarcode_INACTIVEな内包商品は展開されないこと() {
        // Given - 3つ目の商品（INACTIVE）を作成
        Product product3 = new Product();
        product3.setName("商品C");
        product3.setBarcode("3333333333");
        product3.setPrice(new BigDecimal("200"));
        product3.setTaxCategory(Product.TaxCategory.TAXABLE);
        product3.setStatus(Product.Status.INACTIVE);
        entityManager.persist(product3);
        entityManager.flush();

        SetProduct setProduct = new SetProduct();
        setProduct.setSetBarcode("SET_INACTIVE_ITEM");
        setProduct.setSetName("INACTIVE内包セット");
        setProduct.setSetPrice(new BigDecimal("800"));

        Product product1 = entityManager.getReference(Product.class, productId1);
        SetProductItem item1 = new SetProductItem();
        item1.setProduct(product1);
        item1.setQuantity(1);
        setProduct.addItem(item1);

        Product product3Ref = entityManager.getReference(Product.class, product3.getId());
        SetProductItem item3 = new SetProductItem();
        item3.setProduct(product3Ref);
        item3.setQuantity(1);
        setProduct.addItem(item3);

        entityManager.persist(setProduct);
        entityManager.flush();

        // When
        Optional<SetProductExpansion> expansion = setProductService.expandByBarcode("SET_INACTIVE_ITEM");

        // Then - INACTIVEな商品Cは展開されず、ACTIVEな商品Aのみが返される
        assertTrue(expansion.isPresent());
        List<SetProductExpansion.ExpandedItem> items = expansion.get().items();
        assertEquals(1, items.size(), "INACTIVEな内包商品は展開されないべき");
        assertEquals("商品A", items.get(0).name(), "ACTIVEな商品Aのみが展開されるべき");
    }

    @Test
    void expandByBarcode_全内包商品がINACTIVEの場合はOptional_empty_を返すこと() {
        // Given - 2つのINACTIVE商品を作成
        Product product3 = new Product();
        product3.setName("商品C");
        product3.setBarcode("3333333333");
        product3.setPrice(new BigDecimal("200"));
        product3.setTaxCategory(Product.TaxCategory.TAXABLE);
        product3.setStatus(Product.Status.INACTIVE);
        entityManager.persist(product3);

        Product product4 = new Product();
        product4.setName("商品D");
        product4.setBarcode("4444444444");
        product4.setPrice(new BigDecimal("100"));
        product4.setTaxCategory(Product.TaxCategory.TAXABLE);
        product4.setStatus(Product.Status.INACTIVE);
        entityManager.persist(product4);
        entityManager.flush();

        SetProduct setProduct = new SetProduct();
        setProduct.setSetBarcode("SET_ALL_INACTIVE");
        setProduct.setSetName("全INACTIVEセット");
        setProduct.setSetPrice(new BigDecimal("300"));

        Product product3Ref = entityManager.getReference(Product.class, product3.getId());
        SetProductItem item3 = new SetProductItem();
        item3.setProduct(product3Ref);
        item3.setQuantity(1);
        setProduct.addItem(item3);

        Product product4Ref = entityManager.getReference(Product.class, product4.getId());
        SetProductItem item4 = new SetProductItem();
        item4.setProduct(product4Ref);
        item4.setQuantity(1);
        setProduct.addItem(item4);

        entityManager.persist(setProduct);
        entityManager.flush();

        // When
        Optional<SetProductExpansion> expansion = setProductService.expandByBarcode("SET_ALL_INACTIVE");

        // Then - 全内包商品がINACTIVEの場合はOptional.empty()を返す
        assertTrue(expansion.isEmpty(), "全内包商品がINACTIVEの場合はOptional.empty()を返すべき");
    }

    @Test
    void save_セット商品にversionが設定されること() {
        // Given
        SetProduct setProduct = new SetProduct();
        setProduct.setSetBarcode("SET_VERSION");
        setProduct.setSetName("バージョンテスト");
        setProduct.setSetPrice(new BigDecimal("500"));

        Product product1 = entityManager.getReference(Product.class, productId1);
        SetProductItem item1 = new SetProductItem();
        item1.setProduct(product1);
        item1.setQuantity(1);
        setProduct.addItem(item1);

        // When
        SetProduct saved = setProductService.save(setProduct);

        // Then
        assertNotNull(saved.getId());
        assertEquals(Long.valueOf(0L), saved.getVersion(), "新規保存時のversionは0であるべき");
    }

    // === 入力検証テスト ===

    @Test
    void save_nullセット商品でValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            setProductService.save(null);
        });
    }

    @Test
    void save_nullバーコードでValidationExceptionがスローされる() {
        // Given
        SetProduct setProduct = new SetProduct();
        setProduct.setSetName("テストセット");
        setProduct.setSetPrice(new BigDecimal("500"));

        assertThrows(ValidationException.class, () -> {
            setProductService.save(setProduct);
        });
    }

    @Test
    void save_null名前でValidationExceptionがスローされる() {
        // Given
        SetProduct setProduct = new SetProduct();
        setProduct.setSetBarcode("SET001");
        setProduct.setSetPrice(new BigDecimal("500"));

        assertThrows(ValidationException.class, () -> {
            setProductService.save(setProduct);
        });
    }

    @Test
    void save_null価格でValidationExceptionがスローされる() {
        // Given
        SetProduct setProduct = new SetProduct();
        setProduct.setSetBarcode("SET001");
        setProduct.setSetName("テストセット");

        assertThrows(ValidationException.class, () -> {
            setProductService.save(setProduct);
        });
    }

    @Test
    void save_負の価格でValidationExceptionがスローされる() {
        // Given
        SetProduct setProduct = new SetProduct();
        setProduct.setSetBarcode("SET001");
        setProduct.setSetName("テストセット");

        // setSetPriceで負の値を設定するとドメイン層でValidationExceptionがスローされる
        assertThrows(ValidationException.class, () -> {
            setProduct.setSetPrice(new BigDecimal("-500"));
        });
    }

    @Test
    void save_空のアイテムリストでValidationExceptionがスローされる() {
        // Given
        SetProduct setProduct = new SetProduct();
        setProduct.setSetBarcode("SET001");
        setProduct.setSetName("テストセット");
        setProduct.setSetPrice(new BigDecimal("500"));
        // itemsはデフォルトで空のArrayList

        assertThrows(ValidationException.class, () -> {
            setProductService.save(setProduct);
        });
    }
}
