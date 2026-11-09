package com.github.mizo432.reg.product.cqrs.handler;

import com.github.mizo432.reg.product.cqrs.query.GetActiveProductsQuery;
import com.github.mizo432.reg.product.cqrs.query.GetAllProductsQuery;
import com.github.mizo432.reg.product.cqrs.query.GetEffectivePriceQuery;
import com.github.mizo432.reg.product.cqrs.query.GetProductByBarcodeQuery;
import com.github.mizo432.reg.product.cqrs.query.GetProductByIdQuery;
import com.github.mizo432.reg.product.cqrs.query.SearchProductsByNameQuery;
import com.github.mizo432.reg.product.domain.Product;
import com.github.mizo432.reg.product.domain.TimeSale;
import com.github.mizo432.reg.product.repository.ProductRepository;
import com.github.mizo432.reg.product.repository.TimeSaleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductQueryHandlerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TimeSaleRepository timeSaleRepository;

    @InjectMocks
    private ProductQueryHandler handler;

    @Test
    void handle_GetAllProductsQuery_全商品が返されること() {
        // Given
        List<Product> products = List.of(createProduct(1L), createProduct(2L));
        when(productRepository.findAll()).thenReturn(products);

        // When
        List<Product> result = handler.handle(new GetAllProductsQuery());

        // Then
        assertEquals(2, result.size());
        verify(productRepository).findAll();
    }

    @Test
    void handle_GetProductByIdQuery_商品が返されること() {
        // Given
        Product product = createProduct(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // When
        Optional<Product> result = handler.handle(new GetProductByIdQuery(1L));

        // Then
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void handle_GetProductByIdQuery_存在しない商品はemptyを返すこと() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<Product> result = handler.handle(new GetProductByIdQuery(999L));

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void handle_GetProductByBarcodeQuery_商品が返されること() {
        // Given
        Product product = createProduct(1L);
        product.setBarcode("1234567890");
        when(productRepository.findByBarcode("1234567890")).thenReturn(Optional.of(product));

        // When
        Optional<Product> result = handler.handle(new GetProductByBarcodeQuery("1234567890"));

        // Then
        assertTrue(result.isPresent());
        assertEquals("1234567890", result.get().getBarcode());
    }

    @Test
    void handle_SearchProductsByNameQuery_名前一致商品が返されること() {
        // Given
        List<Product> products = List.of(createProduct(1L), createProduct(2L));
        when(productRepository.findByNameContaining("テスト")).thenReturn(products);

        // When
        List<Product> result = handler.handle(new SearchProductsByNameQuery("テスト"));

        // Then
        assertEquals(2, result.size());
    }

    @Test
    void handle_GetActiveProductsQuery_ACTIVE商品のみが返されること() {
        // Given
        Product activeProduct = createProduct(1L);
        activeProduct.setStatus(Product.Status.ACTIVE);
        when(productRepository.findByStatus(Product.Status.ACTIVE)).thenReturn(List.of(activeProduct));

        // When
        List<Product> result = handler.handle(new GetActiveProductsQuery());

        // Then
        assertEquals(1, result.size());
        assertEquals(Product.Status.ACTIVE, result.get(0).getStatus());
    }

    @Test
    void handle_GetEffectivePriceQuery_タイムセールなしで通常価格を返すこと() {
        // Given
        Product product = createProduct(1L);
        product.setPrice(new BigDecimal("1000"));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(timeSaleRepository.findByProductIdAndStatusOrderByStartDateDesc(1L, TimeSale.Status.ACTIVE))
                .thenReturn(List.of());

        // When
        Optional<BigDecimal> result = handler.handle(new GetEffectivePriceQuery(1L));

        // Then
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("1000"), result.get());
    }

    @Test
    void handle_GetEffectivePriceQuery_アクティブなタイムセールでセール価格を返すこと() {
        // Given
        Product product = createProduct(1L);
        product.setPrice(new BigDecimal("1000"));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        TimeSale timeSale = new TimeSale();
        timeSale.setSalePrice(new BigDecimal("800"));
        timeSale.setStartDate(LocalDateTime.now().minusHours(1));
        timeSale.setEndDate(LocalDateTime.now().plusHours(1));
        timeSale.setStatus(TimeSale.Status.ACTIVE);
        when(timeSaleRepository.findByProductIdAndStatusOrderByStartDateDesc(1L, TimeSale.Status.ACTIVE))
                .thenReturn(List.of(timeSale));

        // When
        Optional<BigDecimal> result = handler.handle(new GetEffectivePriceQuery(1L));

        // Then
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("800"), result.get());
    }

    @Test
    void handle_GetEffectivePriceQuery_存在しない商品でemptyを返すこと() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<BigDecimal> result = handler.handle(new GetEffectivePriceQuery(999L));

        // Then
        assertTrue(result.isEmpty());
    }

    private Product createProduct(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setName("テスト商品");
        product.setBarcode("1234567890");
        product.setPrice(new BigDecimal("1000"));
        product.setTaxCategory(Product.TaxCategory.TAXABLE);
        product.setStatus(Product.Status.ACTIVE);
        return product;
    }
}
