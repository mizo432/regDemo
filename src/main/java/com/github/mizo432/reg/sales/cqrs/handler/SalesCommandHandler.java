package com.github.mizo432.reg.sales.cqrs.handler;

import com.github.mizo432.reg.commission.domain.CommissionProduct;
import com.github.mizo432.reg.commission.domain.CommissionRecord;
import com.github.mizo432.reg.commission.repository.CommissionRecordRepository;
import com.github.mizo432.reg.commission.repository.CommissionRepository;
import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.inventory.service.InventoryService;
import com.github.mizo432.reg.product.domain.Product;
import com.github.mizo432.reg.sales.cqrs.command.CancelTransactionCommand;
import com.github.mizo432.reg.sales.cqrs.command.CompleteCheckoutCommand;
import com.github.mizo432.reg.sales.cqrs.command.ReturnTransactionCommand;
import com.github.mizo432.reg.sales.domain.CancelReturn;
import com.github.mizo432.reg.sales.domain.Transaction;
import com.github.mizo432.reg.sales.domain.TransactionItem;
import com.github.mizo432.reg.sales.repository.CancelReturnRepository;
import com.github.mizo432.reg.sales.repository.TransactionRepository;
import com.github.mizo432.reg.sync.service.SyncService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * 販売関連のコマンドを処理するハンドラー
 */
@Component
@Transactional
public class SalesCommandHandler {

    private final TransactionRepository transactionRepository;
    private final CancelReturnRepository cancelReturnRepository;
    private final InventoryService inventoryService;
    private final CommissionRepository commissionRepository;
    private final CommissionRecordRepository commissionRecordRepository;
    private final SyncService syncService;
    private final BigDecimal taxRate;
    private final BigDecimal reducedTaxRate;

    public SalesCommandHandler(TransactionRepository transactionRepository,
                               CancelReturnRepository cancelReturnRepository,
                               InventoryService inventoryService,
                               CommissionRepository commissionRepository,
                               CommissionRecordRepository commissionRecordRepository,
                               SyncService syncService,
                               @Value("${reg.tax.rate:0.1}") BigDecimal taxRate,
                               @Value("${reg.tax.reducedRate:0.08}") BigDecimal reducedTaxRate) {
        this.transactionRepository = transactionRepository;
        this.cancelReturnRepository = cancelReturnRepository;
        this.inventoryService = inventoryService;
        this.commissionRepository = commissionRepository;
        this.commissionRecordRepository = commissionRecordRepository;
        this.syncService = syncService;
        this.taxRate = taxRate;
        this.reducedTaxRate = reducedTaxRate;
    }

    public Transaction execute(CompleteCheckoutCommand command) {
        Transaction transaction = command.transaction();

        // 入力検証
        if (transaction == null) {
            throw new ValidationException("transaction", "トランザクションはnullであってはいけません");
        }
        List<TransactionItem> items = transaction.getItems();
        if (items == null || items.isEmpty()) {
            throw new ValidationException("items", "カートは空であってはいけません");
        }

        // 各アイテムの検証
        for (TransactionItem item : items) {
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("unitPrice", "単価は0以上である必要があります: " + item.getUnitPrice());
            }
            if (item.getQuantity() <= 0) {
                throw new ValidationException("quantity", "数量は1以上である必要があります: " + item.getQuantity());
            }
        }

        // 取引IDを生成
        String transactionId = generateTransactionId();
        transaction.setTransactionId(transactionId);

        // 合計を計算
        calculateTotals(transaction);

        // カート追加時に予約した在庫を確定する
        for (TransactionItem item : transaction.getItems()) {
            inventoryService.confirmReservedStock(item.getProduct().getId(), item.getQuantity());
        }

        // 委託商品の手数料を計算・記録
        for (TransactionItem item : transaction.getItems()) {
            commissionRepository.findByProductIdAndStatus(item.getProduct().getId(), CommissionProduct.Status.ACTIVE)
                    .ifPresent(commissionProduct -> {
                        BigDecimal saleAmount = item.getTotalAmount();
                        BigDecimal commissionAmount = commissionProduct.calculateCommission(saleAmount);
                        BigDecimal payableAmount = saleAmount.subtract(commissionAmount);

                        CommissionRecord record = new CommissionRecord();
                        record.setTransaction(transaction);
                        record.setProduct(item.getProduct());
                        record.setSaleAmount(saleAmount);
                        record.setCommissionAmount(commissionAmount);
                        record.setPayableAmount(payableAmount);
                        commissionRecordRepository.save(record);
                    });
        }

        // 取引を保存
        Transaction savedTransaction = transactionRepository.save(transaction);

        // 同期キューに追加
        syncService.queueTransaction(savedTransaction);

        return savedTransaction;
    }

    public CancelReturn execute(CancelTransactionCommand command) {
        Transaction originalTransaction = transactionRepository.findByTransactionIdForUpdate(command.originalTransactionId())
                .orElseThrow(() -> new IllegalArgumentException("取引が見つかりません: " + command.originalTransactionId()));

        if (originalTransaction.getStatus() == Transaction.Status.CANCELLED) {
            throw new IllegalStateException("取引は既にキャンセル済みです: " + command.originalTransactionId());
        }
        if (originalTransaction.getStatus() == Transaction.Status.RETURNED) {
            throw new IllegalStateException("取引は既に返品済みです: " + command.originalTransactionId());
        }

        originalTransaction.setStatus(Transaction.Status.CANCELLED);
        transactionRepository.save(originalTransaction);

        for (TransactionItem item : originalTransaction.getItems()) {
            inventoryService.increaseStock(item.getProduct().getId(), item.getQuantity());
        }

        // 委託商品の手数料記録を削除する
        List<CommissionRecord> records = commissionRecordRepository.findByTransactionId(originalTransaction.getId());
        if (!records.isEmpty()) {
            commissionRecordRepository.deleteAll(records);
        }

        CancelReturn cancelReturn = new CancelReturn();
        cancelReturn.setOriginalTransactionId(command.originalTransactionId());
        cancelReturn.setTransaction(originalTransaction);
        cancelReturn.setReturnAmount(originalTransaction.getGrandTotal());
        cancelReturn.setReason(command.reason());
        cancelReturn.setOperatorId(command.operatorId());
        cancelReturn.setTerminalId(originalTransaction.getTerminalId());
        cancelReturn.setStatus(CancelReturn.Status.APPROVED);
        return cancelReturnRepository.save(cancelReturn);
    }

    public CancelReturn execute(ReturnTransactionCommand command) {
        Transaction originalTransaction = transactionRepository.findByTransactionIdForUpdate(command.originalTransactionId())
                .orElseThrow(() -> new IllegalArgumentException("取引が見つかりません: " + command.originalTransactionId()));

        if (originalTransaction.getStatus() == Transaction.Status.CANCELLED) {
            throw new IllegalStateException("取引は既にキャンセル済みです: " + command.originalTransactionId());
        }
        if (originalTransaction.getStatus() == Transaction.Status.RETURNED) {
            throw new IllegalStateException("取引は既に返品済みです: " + command.originalTransactionId());
        }

        originalTransaction.setStatus(Transaction.Status.RETURNED);
        transactionRepository.save(originalTransaction);

        for (TransactionItem item : originalTransaction.getItems()) {
            inventoryService.increaseStock(item.getProduct().getId(), item.getQuantity());
        }

        // 委託商品の手数料記録を削除する
        List<CommissionRecord> records = commissionRecordRepository.findByTransactionId(originalTransaction.getId());
        if (!records.isEmpty()) {
            commissionRecordRepository.deleteAll(records);
        }

        CancelReturn cancelReturn = new CancelReturn();
        cancelReturn.setOriginalTransactionId(command.originalTransactionId());
        cancelReturn.setTransaction(originalTransaction);
        cancelReturn.setReturnAmount(originalTransaction.getGrandTotal());
        cancelReturn.setReason(command.reason());
        cancelReturn.setOperatorId(command.operatorId());
        cancelReturn.setTerminalId(originalTransaction.getTerminalId());
        cancelReturn.setStatus(CancelReturn.Status.APPROVED);
        return cancelReturnRepository.save(cancelReturn);
    }

    private void calculateTotals(Transaction transaction) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;

        for (TransactionItem item : transaction.getItems()) {
            BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            item.setTotalAmount(itemTotal);
            subtotal = subtotal.add(itemTotal);

            Product.TaxCategory taxCategory = item.getProduct().getTaxCategory();
            if (taxCategory == Product.TaxCategory.TAXABLE) {
                BigDecimal tax = itemTotal.multiply(taxRate)
                        .setScale(2, RoundingMode.HALF_UP);
                item.setTaxAmount(tax);
                taxTotal = taxTotal.add(tax);
            } else if (taxCategory == Product.TaxCategory.REDUCED) {
                BigDecimal tax = itemTotal.multiply(reducedTaxRate)
                        .setScale(2, RoundingMode.HALF_UP);
                item.setTaxAmount(tax);
                taxTotal = taxTotal.add(tax);
            }
            // NON_TAXABLEの場合は税金を計算しない
        }

        transaction.setSubtotal(subtotal);
        transaction.setTaxTotal(taxTotal);
        transaction.setGrandTotal(subtotal.add(taxTotal));
    }

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().toUpperCase();
    }
}
