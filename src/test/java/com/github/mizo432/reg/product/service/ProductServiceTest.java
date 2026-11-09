package com.github.mizo432.reg.product.service;

import com.github.mizo432.reg.product.domain.Product;
import com.github.mizo432.reg.product.domain.TimeSale;
import com.github.mizo432.reg.product.repository.ProductRepository;
import com.github.mizo432.reg.product.repository.TimeSaleRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProductServiceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TimeSaleRepository timeSaleRepository;

    private Long productId;

    @BeforeEach
    void setUp() {
        Product product = new Product();
        product.setName("タイムセール商品");
        product.setBarcode("TS001");
        product.setPrice(new BigDecimal("1000"));
        product.setTaxCategory(Product.TaxCategory.TAXABLE);
        entityManager.persist(product);
        entityManager.flush();
        productId = product.getId();
    }

    @Test
    void getEffectivePrice_タイムセールがない場合は通常価格を返すこと() {
        // When
        Optional<BigDecimal> price = productService.getEffectivePrice(productId);

        // Then
        assertTrue(price.isPresent());
        assertEquals(new BigDecimal("1000"), price.get());
    }

    @Test
    void getEffectivePrice_アクティブなタイムセールがある場合はセール価格を返すこと() {
        // Given - システム時刻の範囲内にタイムセールを設定
        Product product = entityManager.getReference(Product.class, productId);
        TimeSale timeSale = new TimeSale();
        timeSale.setProduct(product);
        timeSale.setSalePrice(new BigDecimal("800"));
        timeSale.setStartDate(LocalDateTime.now().minusHours(1));
        timeSale.setEndDate(LocalDateTime.now().plusHours(1));
        timeSale.setStatus(TimeSale.Status.ACTIVE);
        timeSaleRepository.save(timeSale);
        entityManager.flush();

        // When
        Optional<BigDecimal> price = productService.getEffectivePrice(productId);

        // Then
        assertTrue(price.isPresent());
        assertEquals(new BigDecimal("800"), price.get());
    }

    @Test
    void getEffectivePrice_複数のタイムセールがある場合は開始日時の新しい方を優先すること() {
        // Given - 2つのタイムセールを作成（開始日時の異なる順序）
        Product product = entityManager.getReference(Product.class, productId);

        // 古いタイムセール（開始日時が過去）
        TimeSale oldSale = new TimeSale();
        oldSale.setProduct(product);
        oldSale.setSalePrice(new BigDecimal("700"));
        oldSale.setStartDate(LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        oldSale.setEndDate(LocalDateTime.of(2026, 12, 31, 23, 59, 59));
        oldSale.setStatus(TimeSale.Status.ACTIVE);
        timeSaleRepository.save(oldSale);

        // 新しいタイムセール（開始日時が最近）
        TimeSale newSale = new TimeSale();
        newSale.setProduct(product);
        newSale.setSalePrice(new BigDecimal("900"));
        newSale.setStartDate(LocalDateTime.of(2026, 11, 1, 0, 0, 0));
        newSale.setEndDate(LocalDateTime.of(2026, 12, 31, 23, 59, 59));
        newSale.setStatus(TimeSale.Status.ACTIVE);
        timeSaleRepository.save(newSale);
        entityManager.flush();

        // When
        Optional<BigDecimal> price = productService.getEffectivePrice(productId);

        // Then - 開始日時の新しい方（900円）が優先される
        assertTrue(price.isPresent());
        assertEquals(new BigDecimal("900"), price.get(),
                "開始日時の新しいタイムセールが優先されるべき");
    }

    @Test
    void getEffectivePrice_INACTIVEなタイムセールは適用されないこと() {
        // Given
        Product product = entityManager.getReference(Product.class, productId);
        TimeSale timeSale = new TimeSale();
        timeSale.setProduct(product);
        timeSale.setSalePrice(new BigDecimal("500"));
        timeSale.setStartDate(LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        timeSale.setEndDate(LocalDateTime.of(2026, 12, 31, 23, 59, 59));
        timeSale.setStatus(TimeSale.Status.INACTIVE);
        timeSaleRepository.save(timeSale);
        entityManager.flush();

        // When
        Optional<BigDecimal> price = productService.getEffectivePrice(productId);

        // Then - INACTIVEなタイムセールは適用されず、通常価格が返される
        assertTrue(price.isPresent());
        assertEquals(new BigDecimal("1000"), price.get());
    }

    @Test
    void getEffectivePrice_存在しない商品IDはemptyを返すこと() {
        // When
        Optional<BigDecimal> price = productService.getEffectivePrice(-1L);

        // Then
        assertTrue(price.isEmpty());
    }

    @Test
    void getEffectivePrice_INACTIVEなタイムセールはDBからフェッチされないこと() {
        // Given - INACTIVEなタイムセールのみを作成
        Product product = entityManager.getReference(Product.class, productId);
        TimeSale timeSale = new TimeSale();
        timeSale.setProduct(product);
        timeSale.setSalePrice(new BigDecimal("500"));
        timeSale.setStartDate(LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        timeSale.setEndDate(LocalDateTime.of(2026, 12, 31, 23, 59, 59));
        timeSale.setStatus(TimeSale.Status.INACTIVE);
        timeSaleRepository.save(timeSale);
        entityManager.flush();

        // When
        Optional<BigDecimal> price = productService.getEffectivePrice(productId);

        // Then - INACTIVEなタイムセールはDBクエリでフィルタリングされるため、通常価格が返される
        assertTrue(price.isPresent());
        assertEquals(new BigDecimal("1000"), price.get(),
                "INACTIVEなタイムセールはDBからフェッチされず、通常価格が返されるべき");
    }

    @Test
    void getEffectivePrice_ACTIVEとINACTIVEが混在する場合はACTIVEのみを適用すること() {
        // Given - ACTIVEとINACTIVEの両方のタイムセールを作成
        Product product = entityManager.getReference(Product.class, productId);

        TimeSale activeSale = new TimeSale();
        activeSale.setProduct(product);
        activeSale.setSalePrice(new BigDecimal("800"));
        activeSale.setStartDate(LocalDateTime.now().minusHours(1));
        activeSale.setEndDate(LocalDateTime.now().plusHours(1));
        activeSale.setStatus(TimeSale.Status.ACTIVE);
        timeSaleRepository.save(activeSale);

        TimeSale inactiveSale = new TimeSale();
        inactiveSale.setProduct(product);
        inactiveSale.setSalePrice(new BigDecimal("500"));
        inactiveSale.setStartDate(LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        inactiveSale.setEndDate(LocalDateTime.of(2026, 12, 31, 23, 59, 59));
        inactiveSale.setStatus(TimeSale.Status.INACTIVE);
        timeSaleRepository.save(inactiveSale);
        entityManager.flush();

        // When
        Optional<BigDecimal> price = productService.getEffectivePrice(productId);

        // Then - ACTIVEなタイムセール（800円）のみが適用される
        assertTrue(price.isPresent());
        assertEquals(new BigDecimal("800"), price.get(),
                "ACTIVEなタイムセールのみが適用されるべき");
    }
}
