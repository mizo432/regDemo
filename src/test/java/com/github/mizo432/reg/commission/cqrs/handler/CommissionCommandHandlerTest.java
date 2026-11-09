package com.github.mizo432.reg.commission.cqrs.handler;

import com.github.mizo432.reg.commission.cqrs.command.CreateCommissionProductCommand;
import com.github.mizo432.reg.commission.domain.CommissionProduct;
import com.github.mizo432.reg.commission.repository.CommissionRepository;
import com.github.mizo432.reg.exception.ValidationException;
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
class CommissionCommandHandlerTest {

    @Mock
    private CommissionRepository commissionRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CommissionCommandHandler handler;

    @Test
    void execute_CreateCommissionProductCommand_委託商品が保存されること() {
        // Given
        Product product = createProduct(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        CommissionProduct savedCommission = new CommissionProduct();
        savedCommission.setId(1L);
        when(commissionRepository.save(any(CommissionProduct.class))).thenReturn(savedCommission);

        CreateCommissionProductCommand command = new CreateCommissionProductCommand(
                1L, "委託者A", new BigDecimal("0.1"), "RATE");

        // When
        CommissionProduct result = handler.execute(command);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());

        ArgumentCaptor<CommissionProduct> captor = ArgumentCaptor.forClass(CommissionProduct.class);
        verify(commissionRepository).save(captor.capture());
        CommissionProduct captured = captor.getValue();
        assertEquals("委託者A", captured.getCommissioner());
        assertEquals(new BigDecimal("0.1"), captured.getCommissionRate());
        assertEquals(CommissionProduct.CommissionType.RATE, captured.getCommissionType());
    }

    // === 敵対的検証: null・空文字列・無効値（修正済み - 検証が追加された） ===

    @Test
    void execute_CreateCommissionProductCommand_null委託者ValidationExceptionがスローされる() {
        // Given
        CreateCommissionProductCommand command = new CreateCommissionProductCommand(
                1L, null, new BigDecimal("0.1"), "RATE");

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_CreateCommissionProductCommand_空文字列の委託者ValidationExceptionがスローされる() {
        // Given
        CreateCommissionProductCommand command = new CreateCommissionProductCommand(
                1L, "", new BigDecimal("0.1"), "RATE");

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_CreateCommissionProductCommand_負の手数料率ValidationExceptionがスローされる() {
        // Given
        CreateCommissionProductCommand command = new CreateCommissionProductCommand(
                1L, "委託者A", new BigDecimal("-0.1"), "RATE");

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_CreateCommissionProductCommand_1を超える手数料率ValidationExceptionがスローされる() {
        // Given
        CreateCommissionProductCommand command = new CreateCommissionProductCommand(
                1L, "委託者A", new BigDecimal("2.0"), "RATE");

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_CreateCommissionProductCommand_null手数料率ValidationExceptionがスローされる() {
        // Given
        CreateCommissionProductCommand command = new CreateCommissionProductCommand(
                1L, "委託者A", null, "RATE");

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_CreateCommissionProductCommand_無効なCommissionTypeでValidationExceptionがスローされる() {
        // Given
        CreateCommissionProductCommand command = new CreateCommissionProductCommand(
                1L, "委託者A", new BigDecimal("0.1"), "INVALID_TYPE");

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_CreateCommissionProductCommand_存在しない商品で例外がスローされる() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        CreateCommissionProductCommand command = new CreateCommissionProductCommand(
                999L, "委託者A", new BigDecimal("0.1"), "RATE");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_CreateCommissionProductCommand_大文字小文字を間違えたCommissionTypeでValidationExceptionがスローされる() {
        // Given
        CreateCommissionProductCommand command = new CreateCommissionProductCommand(
                1L, "委託者A", new BigDecimal("0.1"), "rate");

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_CreateCommissionProductCommand_空白文字列の委託者ValidationExceptionがスローされる() {
        // Given
        CreateCommissionProductCommand command = new CreateCommissionProductCommand(
                1L, "   ", new BigDecimal("0.1"), "RATE");

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    private Product createProduct(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setName("テスト商品");
        product.setBarcode("1234567890");
        product.setPrice(new BigDecimal("1000"));
        product.setTaxCategory(Product.TaxCategory.TAXABLE);
        return product;
    }
}
