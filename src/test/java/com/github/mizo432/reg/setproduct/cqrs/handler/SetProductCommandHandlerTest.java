package com.github.mizo432.reg.setproduct.cqrs.handler;

import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.product.domain.Product;
import com.github.mizo432.reg.product.repository.ProductRepository;
import com.github.mizo432.reg.product.repository.SetProductRepository;
import com.github.mizo432.reg.setproduct.cqrs.command.CreateSetProductCommand;
import com.github.mizo432.reg.setproduct.cqrs.command.DeleteSetProductCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SetProductCommandHandlerTest {

    @Mock
    private SetProductRepository setProductRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private SetProductCommandHandler setProductCommandHandler;

    // === CreateSetProductCommand の入力検証 ===

    @Test
    void createSetProduct_nullバーコードでValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            setProductCommandHandler.execute(new CreateSetProductCommand(
                    null, "セットA", new BigDecimal("1000"),
                    List.of(new CreateSetProductCommand.SetProductItemCommand(1L, 1))));
        });
    }

    @Test
    void createSetProduct_空文字列のバーコードでValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            setProductCommandHandler.execute(new CreateSetProductCommand(
                    "", "セットA", new BigDecimal("1000"),
                    List.of(new CreateSetProductCommand.SetProductItemCommand(1L, 1))));
        });
    }

    @Test
    void createSetProduct_null名前でValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            setProductCommandHandler.execute(new CreateSetProductCommand(
                    "SET001", null, new BigDecimal("1000"),
                    List.of(new CreateSetProductCommand.SetProductItemCommand(1L, 1))));
        });
    }

    @Test
    void createSetProduct_空文字列の名前でValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            setProductCommandHandler.execute(new CreateSetProductCommand(
                    "SET001", "", new BigDecimal("1000"),
                    List.of(new CreateSetProductCommand.SetProductItemCommand(1L, 1))));
        });
    }

    @Test
    void createSetProduct_null価格でValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            setProductCommandHandler.execute(new CreateSetProductCommand(
                    "SET001", "セットA", null,
                    List.of(new CreateSetProductCommand.SetProductItemCommand(1L, 1))));
        });
    }

    @Test
    void createSetProduct_負の価格でValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            setProductCommandHandler.execute(new CreateSetProductCommand(
                    "SET001", "セットA", new BigDecimal("-1000"),
                    List.of(new CreateSetProductCommand.SetProductItemCommand(1L, 1))));
        });
    }

    @Test
    void createSetProduct_空のアイテムリストでValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            setProductCommandHandler.execute(new CreateSetProductCommand(
                    "SET001", "セットA", new BigDecimal("1000"),
                    Collections.emptyList()));
        });
    }

    @Test
    void createSetProduct_nullアイテムリストでValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            setProductCommandHandler.execute(new CreateSetProductCommand(
                    "SET001", "セットA", new BigDecimal("1000"), null));
        });
    }

    @Test
    void createSetProduct_null商品IDでValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            setProductCommandHandler.execute(new CreateSetProductCommand(
                    "SET001", "セットA", new BigDecimal("1000"),
                    List.of(new CreateSetProductCommand.SetProductItemCommand(null, 1))));
        });
    }

    @Test
    void createSetProduct_負の数量でValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            setProductCommandHandler.execute(new CreateSetProductCommand(
                    "SET001", "セットA", new BigDecimal("1000"),
                    List.of(new CreateSetProductCommand.SetProductItemCommand(1L, -1))));
        });
    }

    @Test
    void createSetProduct_0の数量でValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            setProductCommandHandler.execute(new CreateSetProductCommand(
                    "SET001", "セットA", new BigDecimal("1000"),
                    List.of(new CreateSetProductCommand.SetProductItemCommand(1L, 0))));
        });
    }

    // === DeleteSetProductCommand の入力検証 ===

    @Test
    void deleteSetProduct_nullIDでValidationExceptionがスローされる() {
        assertThrows(ValidationException.class, () -> {
            setProductCommandHandler.execute(new DeleteSetProductCommand(null));
        });
    }
}
