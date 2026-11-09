package com.github.mizo432.reg.product.service;

import com.github.mizo432.reg.exception.ValidationException;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class TimeSaleServiceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TimeSaleService timeSaleService;

    @Autowired
    private TimeSaleRepository timeSaleRepository;

    @Autowired
    private ProductRepository productRepository;

    private Long productId;

    @BeforeEach
    void setUp() {
        Product product = new Product();
        product.setName("タイムセール商品");
        product.setBarcode("TS002");
        product.setPrice(new BigDecimal("1000"));
        product.setTaxCategory(Product.TaxCategory.TAXABLE);
        entityManager.persist(product);
        entityManager.flush();
        productId = product.getId();
    }

    @Test
    void findByProductId_INACTIVEなタイムセールは返さないこと() {
        // Given - INACTIVEなタイムセールを作成
        Product product = entityManager.getReference(Product.class, productId);
        TimeSale timeSale = new TimeSale();
        timeSale.setProduct(product);
        timeSale.setSalePrice(new BigDecimal("800"));
        timeSale.setStartDate(LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        timeSale.setEndDate(LocalDateTime.of(2026, 12, 31, 23, 59, 59));
        timeSale.setStatus(TimeSale.Status.INACTIVE);
        timeSaleRepository.save(timeSale);
        entityManager.flush();

        // When
        List<TimeSale> result = timeSaleService.findByProductId(productId);

        // Then - INACTIVEなタイムセールは返されない
        assertTrue(result.isEmpty(), "INACTIVEなタイムセールはfindByProductIdで返されないべき");
    }

    @Test
    void findByProductId_ACTIVEなタイムセールは返すこと() {
        // Given - ACTIVEなタイムセールを作成
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
        List<TimeSale> result = timeSaleService.findByProductId(productId);

        // Then
        assertEquals(1, result.size(), "ACTIVEなタイムセールはfindByProductIdで返されるべき");
        assertEquals(TimeSale.Status.ACTIVE, result.get(0).getStatus());
    }

    @Test
    void findByProductId_ACTIVEとINACTIVEが混在する場合はACTIVEのみを返すこと() {
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
        inactiveSale.setSalePrice(new BigDecimal("700"));
        inactiveSale.setStartDate(LocalDateTime.of(2025, 1, 1, 0, 0, 0));
        inactiveSale.setEndDate(LocalDateTime.of(2025, 12, 31, 23, 59, 59));
        inactiveSale.setStatus(TimeSale.Status.INACTIVE);
        timeSaleRepository.save(inactiveSale);
        entityManager.flush();

        // When
        List<TimeSale> result = timeSaleService.findByProductId(productId);

        // Then - ACTIVEのみが返される
        assertEquals(1, result.size(), "ACTIVEなタイムセールのみが返されるべき");
        assertEquals(TimeSale.Status.ACTIVE, result.get(0).getStatus());
        assertEquals(new BigDecimal("800"), result.get(0).getSalePrice());
    }

    // === 入力検証テスト ===

    @Test
    void createTimeSale_null商品IDでValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            timeSaleService.createTimeSale(null, new BigDecimal("800"),
                    LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        });
    }

    @Test
    void createTimeSale_nullセール価格でValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            timeSaleService.createTimeSale(productId, null,
                    LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        });
    }

    @Test
    void createTimeSale_負のセール価格でValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            timeSaleService.createTimeSale(productId, new BigDecimal("-100"),
                    LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        });
    }

    @Test
    void createTimeSale_null開始日でValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            timeSaleService.createTimeSale(productId, new BigDecimal("800"),
                    null, LocalDateTime.now().plusHours(1));
        });
    }

    @Test
    void createTimeSale_null終了日でValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            timeSaleService.createTimeSale(productId, new BigDecimal("800"),
                    LocalDateTime.now().minusHours(1), null);
        });
    }

    @Test
    void createTimeSale_終了日が開始日より前でValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            timeSaleService.createTimeSale(productId, new BigDecimal("800"),
                    LocalDateTime.now().plusHours(1), LocalDateTime.now().minusHours(1));
        });
    }

    @Test
    void createTimeSale_終了日が開始日と等しいでValidationExceptionがスローされる() {
        LocalDateTime sameTime = LocalDateTime.now();
        assertThrows(ValidationException.class, () -> {
            timeSaleService.createTimeSale(productId, new BigDecimal("800"),
                    sameTime, sameTime);
        });
    }

    @Test
    void save_nullタイムセールでValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            timeSaleService.save(null);
        });
    }

    @Test
    void save_null商品でValidationExceptionがスローされる() {
        TimeSale timeSale = new TimeSale();
        timeSale.setSalePrice(new BigDecimal("800"));
        timeSale.setStartDate(LocalDateTime.now().minusHours(1));
        timeSale.setEndDate(LocalDateTime.now().plusHours(1));
        assertThrows(ValidationException.class, () -> {
            timeSaleService.save(timeSale);
        });
    }

    @Test
    void save_nullセール価格でValidationExceptionがスローされる() {
        TimeSale timeSale = new TimeSale();
        timeSale.setProduct(entityManager.getReference(Product.class, productId));
        timeSale.setStartDate(LocalDateTime.now().minusHours(1));
        timeSale.setEndDate(LocalDateTime.now().plusHours(1));
        assertThrows(ValidationException.class, () -> {
            timeSaleService.save(timeSale);
        });
    }
}
