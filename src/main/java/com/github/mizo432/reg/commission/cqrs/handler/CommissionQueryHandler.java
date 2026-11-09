package com.github.mizo432.reg.commission.cqrs.handler;

import com.github.mizo432.reg.commission.cqrs.query.*;
import com.github.mizo432.reg.commission.domain.CommissionProduct;
import com.github.mizo432.reg.commission.domain.CommissionRecord;
import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.commission.repository.CommissionRecordRepository;
import com.github.mizo432.reg.commission.repository.CommissionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 委託関連のクエリを処理するハンドラー
 */
@Component
@Transactional(readOnly = true)
public class CommissionQueryHandler {

    private final CommissionRepository commissionRepository;
    private final CommissionRecordRepository commissionRecordRepository;

    public CommissionQueryHandler(CommissionRepository commissionRepository,
                                  CommissionRecordRepository commissionRecordRepository) {
        this.commissionRepository = commissionRepository;
        this.commissionRecordRepository = commissionRecordRepository;
    }

    public List<CommissionProduct> handle(GetAllCommissionProductsQuery query) {
        return commissionRepository.findAll();
    }

    public Optional<CommissionProduct> handle(GetCommissionProductByProductIdQuery query) {
        if (query.productId() == null) {
            throw new ValidationException("productId", "商品IDはnullであってはいけません");
        }
        return commissionRepository.findByProductIdAndStatus(query.productId(), CommissionProduct.Status.ACTIVE);
    }

    public List<CommissionProduct> handle(GetActiveCommissionProductsQuery query) {
        return commissionRepository.findByStatus(CommissionProduct.Status.ACTIVE);
    }

    public List<CommissionRecord> handle(GetCommissionRecordsByTransactionIdQuery query) {
        if (query.transactionId() == null) {
            throw new ValidationException("transactionId", "トランザクションIDはnullであってはいけません");
        }
        return commissionRecordRepository.findByTransactionId(query.transactionId());
    }

    public BigDecimal handle(CalculateCommissionQuery query) {
        if (query.productId() == null) {
            throw new ValidationException("productId", "商品IDはnullであってはいけません");
        }
        if (query.saleAmount() == null) {
            throw new ValidationException("saleAmount", "販売額はnullであってはいけません");
        }
        return commissionRepository.findByProductIdAndStatus(query.productId(), CommissionProduct.Status.ACTIVE)
                .map(cp -> cp.calculateCommission(query.saleAmount()))
                .orElse(BigDecimal.ZERO);
    }
}
