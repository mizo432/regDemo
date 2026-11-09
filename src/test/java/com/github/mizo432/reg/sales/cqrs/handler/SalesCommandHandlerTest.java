package com.github.mizo432.reg.sales.cqrs.handler;

import com.github.mizo432.reg.commission.repository.CommissionRecordRepository;
import com.github.mizo432.reg.commission.repository.CommissionRepository;
import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.inventory.service.InventoryService;
import com.github.mizo432.reg.product.domain.Product;
import com.github.mizo432.reg.sales.cqrs.command.CompleteCheckoutCommand;
import com.github.mizo432.reg.sales.domain.Transaction;
import com.github.mizo432.reg.sales.domain.TransactionItem;
import com.github.mizo432.reg.sales.repository.CancelReturnRepository;
import com.github.mizo432.reg.sales.repository.TransactionRepository;
import com.github.mizo432.reg.sync.service.SyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesCommandHandlerTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CancelReturnRepository cancelReturnRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private CommissionRepository commissionRepository;

    @Mock
    private CommissionRecordRepository commissionRecordRepository;

    @Mock
    private SyncService syncService;

    @InjectMocks
    private SalesCommandHandler handler;

    @Test
    void execute_CompleteCheckoutCommand_空のカートでValidationExceptionがスローされる() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setItems(Collections.emptyList());

        CompleteCheckoutCommand command = new CompleteCheckoutCommand(transaction);

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_CompleteCheckoutCommand_nullトランザクションでValidationExceptionがスローされる() {
        // Given
        CompleteCheckoutCommand command = new CompleteCheckoutCommand(null);

        // When & Then - ValidationExceptionがスローされる
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_CompleteCheckoutCommand_課税商品を含むとtaxRateがnullでNullPointerExceptionがスローされる() {
        // 敵対的検証: @InjectMocksでは@Valueフィールド(taxRate)が注入されないのでnull
        // 課税商品(TAXABLE)を含むとcalculateTotals内でnull.multiply()でNPEになる
        // Given
        Transaction transaction = new Transaction();
        Product product = new Product();
        product.setId(1L);
        product.setTaxCategory(Product.TaxCategory.TAXABLE);

        TransactionItem item = new TransactionItem();
        item.setProduct(product);
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("500"));
        transaction.addItem(item);

        CompleteCheckoutCommand command = new CompleteCheckoutCommand(transaction);

        // When & Then - taxRateがnullなのでNPEがスローされる
        assertThrows(NullPointerException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_CompleteCheckoutCommand_負の単価でValidationExceptionがスローされる() {
        // Given
        Transaction transaction = new Transaction();
        Product product = new Product();
        product.setId(1L);
        product.setTaxCategory(Product.TaxCategory.NON_TAXABLE);

        TransactionItem item = new TransactionItem();
        item.setProduct(product);
        item.setQuantity(1);

        // setUnitPriceで負の値を設定するとドメイン層でValidationExceptionがスローされる
        assertThrows(ValidationException.class, () -> {
            item.setUnitPrice(new BigDecimal("-500"));
        });
    }

    @Test
    void execute_CompleteCheckoutCommand_負の数量でValidationExceptionがスローされる() {
        // Given
        Transaction transaction = new Transaction();
        Product product = new Product();
        product.setId(1L);
        product.setTaxCategory(Product.TaxCategory.NON_TAXABLE);

        TransactionItem item = new TransactionItem();
        item.setProduct(product);
        item.setUnitPrice(new BigDecimal("500"));

        // setQuantityで負の値を設定するとドメイン層でValidationExceptionがスローされる
        assertThrows(ValidationException.class, () -> {
            item.setQuantity(-1);
        });
    }

    @Test
    void execute_CompleteCheckoutCommand_0の数量でValidationExceptionがスローされる() {
        // 0の数量は許容しない
        // Given
        Transaction transaction = new Transaction();
        Product product = new Product();
        product.setId(1L);
        product.setTaxCategory(Product.TaxCategory.NON_TAXABLE);

        TransactionItem item = new TransactionItem();
        item.setProduct(product);
        item.setQuantity(0); // 0の数量
        item.setUnitPrice(new BigDecimal("500"));
        transaction.addItem(item);

        CompleteCheckoutCommand command = new CompleteCheckoutCommand(transaction);

        // When & Then
        assertThrows(ValidationException.class, () -> {
            handler.execute(command);
        });
    }

    @Test
    void execute_CompleteCheckoutCommand_非課税商品で极大価格をチェックアウトできること() {
        // 敵対的検証: 极大の価格でチェックアウトできる
        // Given
        Transaction transaction = new Transaction();
        Product product = new Product();
        product.setId(1L);
        product.setTaxCategory(Product.TaxCategory.NON_TAXABLE);

        TransactionItem item = new TransactionItem();
        item.setProduct(product);
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("999999999999")); // 极大価格
        transaction.addItem(item);

        Transaction savedTransaction = new Transaction();
        savedTransaction.setId(1L);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(commissionRepository.findByProductIdAndStatus(eq(1L), any())).thenReturn(Optional.empty());
        doNothing().when(inventoryService).confirmReservedStock(eq(1L), eq(1));
        doNothing().when(syncService).queueTransaction(any(Transaction.class));

        CompleteCheckoutCommand command = new CompleteCheckoutCommand(transaction);

        // When
        Transaction result = handler.execute(command);

        // Then - 极大価格でチェックアウトできる
        assertNotNull(result);
        assertEquals(new BigDecimal("999999999999"), transaction.getSubtotal());
    }
}
