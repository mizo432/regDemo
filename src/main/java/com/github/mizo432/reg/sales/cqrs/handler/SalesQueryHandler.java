package com.github.mizo432.reg.sales.cqrs.handler;

import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.sales.cqrs.query.GetAllTransactionsQuery;
import com.github.mizo432.reg.sales.cqrs.query.GetCancelReturnsByTransactionIdQuery;
import com.github.mizo432.reg.sales.cqrs.query.GetTransactionByIdQuery;
import com.github.mizo432.reg.sales.cqrs.query.GetTransactionsByStatusQuery;
import com.github.mizo432.reg.sales.domain.CancelReturn;
import com.github.mizo432.reg.sales.domain.Transaction;
import com.github.mizo432.reg.sales.repository.CancelReturnRepository;
import com.github.mizo432.reg.sales.repository.TransactionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 販売関連のクエリを処理するハンドラー
 */
@Component
@Transactional(readOnly = true)
public class SalesQueryHandler {

    private final TransactionRepository transactionRepository;
    private final CancelReturnRepository cancelReturnRepository;

    public SalesQueryHandler(TransactionRepository transactionRepository,
                            CancelReturnRepository cancelReturnRepository) {
        this.transactionRepository = transactionRepository;
        this.cancelReturnRepository = cancelReturnRepository;
    }

    public List<Transaction> handle(GetAllTransactionsQuery query) {
        return transactionRepository.findAllByOrderByDateTimeDesc();
    }

    public Optional<Transaction> handle(GetTransactionByIdQuery query) {
        if (query.transactionId() == null) {
            throw new ValidationException("transactionId", "トランザクションIDはnullであってはいけません");
        }
        return transactionRepository.findByTransactionId(query.transactionId());
    }

    public List<Transaction> handle(GetTransactionsByStatusQuery query) {
        if (query.status() == null) {
            throw new ValidationException("status", "ステータスはnullであってはいけません");
        }
        return transactionRepository.findByStatus(query.status());
    }

    public List<CancelReturn> handle(GetCancelReturnsByTransactionIdQuery query) {
        if (query.originalTransactionId() == null) {
            throw new ValidationException("originalTransactionId", "元のトランザクションIDはnullであってはいけません");
        }
        return cancelReturnRepository.findByOriginalTransactionId(query.originalTransactionId());
    }
}
