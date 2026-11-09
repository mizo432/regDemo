package com.github.mizo432.reg.sales.service;

import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.sales.cqrs.command.CancelTransactionCommand;
import com.github.mizo432.reg.sales.cqrs.command.CompleteCheckoutCommand;
import com.github.mizo432.reg.sales.cqrs.command.ReturnTransactionCommand;
import com.github.mizo432.reg.sales.cqrs.handler.SalesCommandHandler;
import com.github.mizo432.reg.sales.cqrs.handler.SalesQueryHandler;
import com.github.mizo432.reg.sales.cqrs.query.GetAllTransactionsQuery;
import com.github.mizo432.reg.sales.cqrs.query.GetCancelReturnsByTransactionIdQuery;
import com.github.mizo432.reg.sales.cqrs.query.GetTransactionByIdQuery;
import com.github.mizo432.reg.sales.cqrs.query.GetTransactionsByStatusQuery;
import com.github.mizo432.reg.sales.domain.CancelReturn;
import com.github.mizo432.reg.sales.domain.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * 販売サービスのファサード。
 * CQRSパターンを採用し、コマンド操作はSalesCommandHandler、クエリ操作はSalesQueryHandlerに委譲します。
 * 既存のAPIを維持するため、このファサードクラスを介してアクセスします。
 */
@Service
public class SalesService {

    private final SalesCommandHandler commandHandler;
    private final SalesQueryHandler queryHandler;

    public SalesService(SalesCommandHandler commandHandler, SalesQueryHandler queryHandler) {
        this.commandHandler = commandHandler;
        this.queryHandler = queryHandler;
    }

    public List<Transaction> findAll() {
        return queryHandler.handle(new GetAllTransactionsQuery());
    }

    public Optional<Transaction> findByTransactionId(String transactionId) {
        if (transactionId == null || !StringUtils.hasText(transactionId)) {
            throw new ValidationException("transactionId", "トランザクションIDは空であってはいけません");
        }
        return queryHandler.handle(new GetTransactionByIdQuery(transactionId));
    }

    public List<Transaction> findByStatus(Transaction.Status status) {
        if (status == null) {
            throw new ValidationException("status", "ステータスはnullであってはいけません");
        }
        return queryHandler.handle(new GetTransactionsByStatusQuery(status));
    }

    public Transaction completeCheckout(Transaction transaction) {
        if (transaction == null) {
            throw new ValidationException("transaction", "トランザクションはnullであってはいけません");
        }
        return commandHandler.execute(new CompleteCheckoutCommand(transaction));
    }

    public CancelReturn cancelTransaction(String originalTransactionId, String reason, String operatorId) {
        if (originalTransactionId == null || !StringUtils.hasText(originalTransactionId)) {
            throw new ValidationException("originalTransactionId", "元のトランザクションIDは空であってはいけません");
        }
        return commandHandler.execute(new CancelTransactionCommand(originalTransactionId, reason, operatorId));
    }

    public CancelReturn returnTransaction(String originalTransactionId, String reason, String operatorId) {
        if (originalTransactionId == null || !StringUtils.hasText(originalTransactionId)) {
            throw new ValidationException("originalTransactionId", "元のトランザクションIDは空であってはいけません");
        }
        return commandHandler.execute(new ReturnTransactionCommand(originalTransactionId, reason, operatorId));
    }

    public List<CancelReturn> findCancelReturnsByOriginalTransactionId(String originalTransactionId) {
        if (originalTransactionId == null || !StringUtils.hasText(originalTransactionId)) {
            throw new ValidationException("originalTransactionId", "元のトランザクションIDは空であってはいけません");
        }
        return queryHandler.handle(new GetCancelReturnsByTransactionIdQuery(originalTransactionId));
    }
}
