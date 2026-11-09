package com.github.mizo432.reg.commission.service;

import com.github.mizo432.reg.commission.domain.CommissionProduct;
import com.github.mizo432.reg.commission.repository.CommissionRecordRepository;
import com.github.mizo432.reg.commission.repository.CommissionRepository;
import com.github.mizo432.reg.product.domain.Product;
import com.github.mizo432.reg.product.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CommissionServiceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CommissionService commissionService;

    @Autowired
    private CommissionRepository commissionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CommissionRecordRepository commissionRecordRepository;

    private Long productId;

    @BeforeEach
    void setUp() {
        // テスト用の商品を作成
        Product product = new Product();
        product.setName("委託商品");
        product.setBarcode("COMM001");
        product.setPrice(new BigDecimal("1000"));
        product.setTaxCategory(Product.TaxCategory.TAXABLE);
        entityManager.persist(product);
        entityManager.flush();
        productId = product.getId();
    }

    @Test
    void calculateCommission_ACTIVE設定は手数料を計算すること() {
        // Given - ACTIVEなコミッション設定を作成
        Product product = entityManager.getReference(Product.class, productId);
        CommissionProduct commissionProduct = new CommissionProduct();
        commissionProduct.setProduct(product);
        commissionProduct.setCommissioner("委託者A");
        commissionProduct.setCommissionRate(new BigDecimal("0.1"));
        commissionProduct.setCommissionType(CommissionProduct.CommissionType.RATE);
        commissionProduct.setStatus(CommissionProduct.Status.ACTIVE);
        commissionRepository.save(commissionProduct);
        entityManager.flush();

        // When
        BigDecimal commission = commissionService.calculateCommission(productId, new BigDecimal("1000"));

        // Then
        assertEquals(new BigDecimal("100").setScale(2, java.math.RoundingMode.HALF_UP), commission.setScale(2, java.math.RoundingMode.HALF_UP));
    }

    @Test
    void calculateCommission_INACTIVE設定は手数料を計算しないこと() {
        // Given - INACTIVEなコミッション設定を作成
        Product product = entityManager.getReference(Product.class, productId);
        CommissionProduct commissionProduct = new CommissionProduct();
        commissionProduct.setProduct(product);
        commissionProduct.setCommissioner("委託者A");
        commissionProduct.setCommissionRate(new BigDecimal("0.1"));
        commissionProduct.setCommissionType(CommissionProduct.CommissionType.RATE);
        commissionProduct.setStatus(CommissionProduct.Status.INACTIVE);
        commissionRepository.save(commissionProduct);
        entityManager.flush();

        // When
        BigDecimal commission = commissionService.calculateCommission(productId, new BigDecimal("1000"));

        // Then - INACTIVEな設定は手数料を計算しない（0を返す）
        assertEquals(BigDecimal.ZERO, commission);
    }

    @Test
    void findByProductIdActive_INACTIVE設定は返さないこと() {
        // Given - INACTIVEなコミッション設定を作成
        Product product = entityManager.getReference(Product.class, productId);
        CommissionProduct commissionProduct = new CommissionProduct();
        commissionProduct.setProduct(product);
        commissionProduct.setCommissioner("委託者A");
        commissionProduct.setCommissionRate(new BigDecimal("0.1"));
        commissionProduct.setCommissionType(CommissionProduct.CommissionType.RATE);
        commissionProduct.setStatus(CommissionProduct.Status.INACTIVE);
        commissionRepository.save(commissionProduct);
        entityManager.flush();

        // When
        var result = commissionRepository.findByProductIdAndStatus(productId, CommissionProduct.Status.ACTIVE);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void findByProductIdActive_ACTIVE設定は返すこと() {
        // Given - ACTIVEなコミッション設定を作成
        Product product = entityManager.getReference(Product.class, productId);
        CommissionProduct commissionProduct = new CommissionProduct();
        commissionProduct.setProduct(product);
        commissionProduct.setCommissioner("委託者A");
        commissionProduct.setCommissionRate(new BigDecimal("0.1"));
        commissionProduct.setCommissionType(CommissionProduct.CommissionType.RATE);
        commissionProduct.setStatus(CommissionProduct.Status.ACTIVE);
        commissionRepository.save(commissionProduct);
        entityManager.flush();

        // When
        var result = commissionRepository.findByProductIdAndStatus(productId, CommissionProduct.Status.ACTIVE);

        // Then
        assertTrue(result.isPresent());
        assertEquals(CommissionProduct.Status.ACTIVE, result.get().getStatus());
    }

    @Test
    void calculateCommission_RATEタイプで端数が正しく丸められること() {
        // Given - commissionRateがDECIMAL(5,4)で、結果のスケールが不定になるケース
        // 1000 × 0.1234 = 123.4000 → setScale(2) で 123.40 になるべき
        Product product = entityManager.getReference(Product.class, productId);
        CommissionProduct commissionProduct = new CommissionProduct();
        commissionProduct.setProduct(product);
        commissionProduct.setCommissioner("委託者A");
        commissionProduct.setCommissionRate(new BigDecimal("0.1234"));
        commissionProduct.setCommissionType(CommissionProduct.CommissionType.RATE);
        commissionProduct.setStatus(CommissionProduct.Status.ACTIVE);
        commissionRepository.save(commissionProduct);
        entityManager.flush();

        // When
        BigDecimal commission = commissionProduct.calculateCommission(new BigDecimal("1000"));

        // Then - setScale(2, HALF_UP) が適用され、スケールが2であること
        assertEquals(2, commission.scale(), "手数料のスケールは2であるべき");
        assertEquals(new BigDecimal("123.40"), commission);
    }

    @Test
    void calculateCommission_RATEタイプで端数がHALF_UPで丸められること() {
        // Given - 端数が0.005のケース（HALF_UPで切り上げられるべき）
        // 1000 × 0.10005 = 100.05 → setScale(2, HALF_UP) で 100.05
        // 100 × 0.10005 = 10.005 → setScale(2, HALF_UP) で 10.01
        Product product = entityManager.getReference(Product.class, productId);
        CommissionProduct commissionProduct = new CommissionProduct();
        commissionProduct.setProduct(product);
        commissionProduct.setCommissioner("委託者A");
        commissionProduct.setCommissionRate(new BigDecimal("0.10005"));
        commissionProduct.setCommissionType(CommissionProduct.CommissionType.RATE);
        commissionProduct.setStatus(CommissionProduct.Status.ACTIVE);
        commissionRepository.save(commissionProduct);
        entityManager.flush();

        // When
        BigDecimal commission = commissionProduct.calculateCommission(new BigDecimal("100"));

        // Then - 10.005 → HALF_UP で 10.01
        assertEquals(2, commission.scale(), "手数料のスケールは2であるべき");
        assertEquals(new BigDecimal("10.01"), commission, "端数がHALF_UPで切り上げられるべき");
    }

    @Test
    void findByProductId_INACTIVE設定は返さないこと() {
        // Given - INACTIVEなコミッション設定を作成
        Product product = entityManager.getReference(Product.class, productId);
        CommissionProduct commissionProduct = new CommissionProduct();
        commissionProduct.setProduct(product);
        commissionProduct.setCommissioner("委託者A");
        commissionProduct.setCommissionRate(new BigDecimal("0.1"));
        commissionProduct.setCommissionType(CommissionProduct.CommissionType.RATE);
        commissionProduct.setStatus(CommissionProduct.Status.INACTIVE);
        commissionRepository.save(commissionProduct);
        entityManager.flush();

        // When - findByProductIdはACTIVEのみを返すように修正済み
        var result = commissionService.findByProductId(productId);

        // Then - INACTIVEな設定は返されない
        assertTrue(result.isEmpty(), "INACTIVEなコミッション設定はfindByProductIdで返されないべき");
    }

    @Test
    void findByProductId_ACTIVE設定は返すこと() {
        // Given - ACTIVEなコミッション設定を作成
        Product product = entityManager.getReference(Product.class, productId);
        CommissionProduct commissionProduct = new CommissionProduct();
        commissionProduct.setProduct(product);
        commissionProduct.setCommissioner("委託者A");
        commissionProduct.setCommissionRate(new BigDecimal("0.1"));
        commissionProduct.setCommissionType(CommissionProduct.CommissionType.RATE);
        commissionProduct.setStatus(CommissionProduct.Status.ACTIVE);
        commissionRepository.save(commissionProduct);
        entityManager.flush();

        // When
        var result = commissionService.findByProductId(productId);

        // Then
        assertTrue(result.isPresent(), "ACTIVEなコミッション設定はfindByProductIdで返されるべき");
        assertEquals(CommissionProduct.Status.ACTIVE, result.get().getStatus());
    }
}
