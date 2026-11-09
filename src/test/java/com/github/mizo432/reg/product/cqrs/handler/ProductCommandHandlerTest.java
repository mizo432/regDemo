package com.github.mizo432.reg.product.cqrs.handler;

import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.product.cqrs.command.CreateProductCommand;
import com.github.mizo432.reg.product.cqrs.command.DeleteProductCommand;
import com.github.mizo432.reg.product.cqrs.command.UpdateProductCommand;
import com.github.mizo432.reg.product.domain.Product;
import com.github.mizo432.reg.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductCommandHandlerTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductCommandHandler handler;

    @Test
    void execute_CreateProductCommand_商品が保存されること() {
        // Given
        CreateProductCommand command = new CreateProductCommand(
                "テスト商品", "1234567890", new BigDecimal("1000"), "TAXABLE");

        Product savedProduct = new Product();
        savedProduct.setId(1L);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        // When
        Product result = handler.execute(command);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product captured = captor.getValue();
        assertEquals("テスト商品", captured.getName());
        assertEquals("1234567890", captured.getBarcode());
        assertEquals(new BigDecimal("1000"), captured.getPrice());
        assertEquals(Product.TaxCategory.TAXABLE, captured.getTaxCategory());
        assertEquals(Product.Status.ACTIVE, captured.getStatus());
    }

    @Test
    void execute_CreateProductCommand_非課税商品が正しく保存されること() {
        // Given
        CreateProductCommand command = new CreateProductCommand(
                "非課税商品", "0000000000", new BigDecimal("500"), "NON_TAXABLE");

        Product savedProduct = new Product();
        savedProduct.setId(2L);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        // When
        handler.execute(command);

        // Then
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertEquals(Product.TaxCategory.NON_TAXABLE, captor.getValue().getTaxCategory());
    }

    @Test
    void execute_UpdateProductCommand_商品が更新されること() {
        // Given
        Product existingProduct = new Product();
        existingProduct.setId(1L);
        existingProduct.setName("旧商品");
        existingProduct.setBarcode("1111111111");
        existingProduct.setPrice(new BigDecimal("500"));
        existingProduct.setTaxCategory(Product.TaxCategory.NON_TAXABLE);
        existingProduct.setStatus(Product.Status.ACTIVE);

        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);

        UpdateProductCommand command = new UpdateProductCommand(
                1L, "新商品", "2222222222", new BigDecimal("1000"), "TAXABLE", "INACTIVE");

        // When
        Product result = handler.execute(command);

        // Then
        assertEquals("新商品", result.getName());
        assertEquals("2222222222", result.getBarcode());
        assertEquals(new BigDecimal("1000"), result.getPrice());
        assertEquals(Product.TaxCategory.TAXABLE, result.getTaxCategory());
        assertEquals(Product.Status.INACTIVE, result.getStatus());
    }

    @Test
    void execute_UpdateProductCommand_存在しない商品で例外がスローされる() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateProductCommand command = new UpdateProductCommand(
                999L, "商品", "111", new BigDecimal("100"), "TAXABLE", "ACTIVE");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_DeleteProductCommand_商品が削除されること() {
        // Given
        DeleteProductCommand command = new DeleteProductCommand(1L);
        doNothing().when(productRepository).deleteById(1L);

        // When
        handler.execute(command);

        // Then
        verify(productRepository).deleteById(1L);
    }

    // === 敵対的検証: null・空文字列・無効値（修正済み - 検証が追加された） ===

    @Test
    void execute_CreateProductCommand_null名前ValidationExceptionがスローされる() {
        // Given
        CreateProductCommand command = new CreateProductCommand(
                null, "1234567890", new BigDecimal("1000"), "TAXABLE");

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_CreateProductCommand_空文字列の名前ValidationExceptionがスローされる() {
        // Given
        CreateProductCommand command = new CreateProductCommand(
                "", "1234567890", new BigDecimal("1000"), "TAXABLE");

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_CreateProductCommand_空白文字列の名前ValidationExceptionがスローされる() {
        // Given
        CreateProductCommand command = new CreateProductCommand(
                "   ", "1234567890", new BigDecimal("1000"), "TAXABLE");

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_CreateProductCommand_nullバーコードValidationExceptionがスローされる() {
        // Given
        CreateProductCommand command = new CreateProductCommand(
                "商品", null, new BigDecimal("1000"), "TAXABLE");

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_CreateProductCommand_空文字列のバーコードValidationExceptionがスローされる() {
        // Given
        CreateProductCommand command = new CreateProductCommand(
                "商品", "", new BigDecimal("1000"), "TAXABLE");

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_CreateProductCommand_負の価格ValidationExceptionがスローされる() {
        // Given
        CreateProductCommand command = new CreateProductCommand(
                "商品", "1234567890", new BigDecimal("-1000"), "TAXABLE");

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_CreateProductCommand_0の価格が保存されること() {
        // 0円商品は有効なケースがあるため許容
        // Given
        CreateProductCommand command = new CreateProductCommand(
                "商品", "1234567890", BigDecimal.ZERO, "TAXABLE");

        Product savedProduct = new Product();
        savedProduct.setId(1L);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        // When
        handler.execute(command);

        // Then
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertEquals(BigDecimal.ZERO, captor.getValue().getPrice());
    }

    @Test
    void execute_CreateProductCommand_null価格ValidationExceptionがスローされる() {
        // Given
        CreateProductCommand command = new CreateProductCommand(
                "商品", "1234567890", null, "TAXABLE");

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_CreateProductCommand_無効なTaxCategoryでValidationExceptionがスローされる() {
        // Given
        CreateProductCommand command = new CreateProductCommand(
                "商品", "1234567890", new BigDecimal("1000"), "INVALID_TAX");

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_CreateProductCommand_大文字小文字を間違えたTaxCategoryでValidationExceptionがスローされる() {
        // Given
        CreateProductCommand command = new CreateProductCommand(
                "商品", "1234567890", new BigDecimal("1000"), "taxable");

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_UpdateProductCommand_無効なStatusでValidationExceptionがスローされる() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(new Product()));

        UpdateProductCommand command = new UpdateProductCommand(
                1L, "商品", "111", new BigDecimal("100"), "TAXABLE", "INVALID_STATUS");

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_UpdateProductCommand_負の価格でValidationExceptionがスローされる() {
        // Given
        Product existingProduct = new Product();
        existingProduct.setId(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));

        UpdateProductCommand command = new UpdateProductCommand(
                1L, "商品", "111", new BigDecimal("-500"), "TAXABLE", "ACTIVE");

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_CreateProductCommand_超长バーコードが保存されること() {
        // 超长文字列の検証はDB制約に任せている（アプリケーション層では検証しない）
        // Given
        String longBarcode = "1".repeat(200);
        CreateProductCommand command = new CreateProductCommand(
                "商品", longBarcode, new BigDecimal("1000"), "TAXABLE");

        Product savedProduct = new Product();
        savedProduct.setId(1L);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        // When
        handler.execute(command);

        // Then
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertEquals(200, captor.getValue().getBarcode().length());
    }

    @Test
    void execute_CreateProductCommand_超长名前が保存されること() {
        // 超长文字列の検証はDB制約に任せている（アプリケーション層では検証しない）
        // Given
        String longName = "商".repeat(300);
        CreateProductCommand command = new CreateProductCommand(
                longName, "1234567890", new BigDecimal("1000"), "TAXABLE");

        Product savedProduct = new Product();
        savedProduct.setId(1L);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        // When
        handler.execute(command);

        // Then
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertEquals(300, captor.getValue().getName().length());
    }

    @Test
    void execute_CreateProductCommand_SQLインジェクション文字列が保存されること() {
        // SQLインジェクション文字列はJPAパラメータ化クエリでDBは安全
        // アプリケーション層では入力サニタイズを行わない（DB制約に依存）
        // Given
        String maliciousName = "'; DROP TABLE product; --";
        CreateProductCommand command = new CreateProductCommand(
                maliciousName, "1234567890", new BigDecimal("1000"), "TAXABLE");

        Product savedProduct = new Product();
        savedProduct.setId(1L);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        // When
        handler.execute(command);

        // Then
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertEquals(maliciousName, captor.getValue().getName());
    }
}
