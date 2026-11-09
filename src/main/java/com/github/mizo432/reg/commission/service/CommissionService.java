package com.github.mizo432.reg.commission.service;

import com.github.mizo432.reg.commission.cqrs.command.CreateCommissionProductCommand;
import com.github.mizo432.reg.commission.cqrs.command.DeleteCommissionProductCommand;
import com.github.mizo432.reg.commission.cqrs.handler.CommissionCommandHandler;
import com.github.mizo432.reg.commission.cqrs.handler.CommissionQueryHandler;
import com.github.mizo432.reg.commission.cqrs.query.*;
import com.github.mizo432.reg.commission.domain.CommissionProduct;
import com.github.mizo432.reg.commission.domain.CommissionRecord;
import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.commission.repository.CommissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 委託サービスのファサード。
 * CQRSパターンを採用し、コマンド操作はCommissionCommandHandler、クエリ操作はCommissionQueryHandlerに委譲します。
 * 既存のAPIを維持するため、このファサードクラスを介してアクセスします。
 */
@Service
public class CommissionService {

    private final CommissionCommandHandler commandHandler;
    private final CommissionQueryHandler queryHandler;
    private final CommissionRepository commissionRepository;

    public CommissionService(CommissionCommandHandler commandHandler,
                            CommissionQueryHandler queryHandler,
                            CommissionRepository commissionRepository) {
        this.commandHandler = commandHandler;
        this.queryHandler = queryHandler;
        this.commissionRepository = commissionRepository;
    }

    public List<CommissionProduct> findAll() {
        return queryHandler.handle(new GetAllCommissionProductsQuery());
    }

    public Optional<CommissionProduct> findByProductId(Long productId) {
        return queryHandler.handle(new GetCommissionProductByProductIdQuery(productId));
    }

    public List<CommissionProduct> findActiveCommissionProducts() {
        return queryHandler.handle(new GetActiveCommissionProductsQuery());
    }

    public List<CommissionRecord> findRecordsByTransactionId(Long transactionId) {
        return queryHandler.handle(new GetCommissionRecordsByTransactionIdQuery(transactionId));
    }

    public CommissionProduct save(CommissionProduct commissionProduct) {
        // 入力検証
        if (commissionProduct == null) {
            throw new ValidationException("commissionProduct", "委託商品はnullであってはいけません");
        }
        if (commissionProduct.getProduct() == null) {
            throw new ValidationException("product", "商品はnullであってはいけません");
        }
        if (commissionProduct.getCommissioner() == null || !StringUtils.hasText(commissionProduct.getCommissioner())) {
            throw new ValidationException("commissioner", "委託者は空であってはいけません");
        }
        if (commissionProduct.getCommissionType() == null) {
            throw new ValidationException("commissionType", "手数料タイプはnullであってはいけません");
        }
        if (commissionProduct.getCommissionType() == CommissionProduct.CommissionType.RATE && commissionProduct.getCommissionRate() == null) {
            throw new ValidationException("commissionRate", "RATEタイプでは手数料率が設定されている必要があります");
        }
        if (commissionProduct.getCommissionRate() != null && commissionProduct.getCommissionRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("commissionRate", "手数料率は0以上である必要があります: " + commissionProduct.getCommissionRate());
        }
        if (commissionProduct.getCommissionRate() != null && commissionProduct.getCommissionRate().compareTo(BigDecimal.ONE) > 0) {
            throw new ValidationException("commissionRate", "手数料率は100%以下である必要があります: " + commissionProduct.getCommissionRate());
        }
        if (commissionProduct.getCommissionType() == CommissionProduct.CommissionType.FIXED && commissionProduct.getFixedCommission() == null) {
            throw new ValidationException("fixedCommission", "FIXEDタイプでは固定額が設定されている必要があります");
        }
        if (commissionProduct.getFixedCommission() != null && commissionProduct.getFixedCommission().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("fixedCommission", "固定額は0以上である必要があります: " + commissionProduct.getFixedCommission());
        }
        return commissionRepository.save(commissionProduct);
    }

    public CommissionProduct createCommissionProduct(Long productId, String commissioner,
                                                      BigDecimal commissionRate,
                                                      CommissionProduct.CommissionType commissionType) {
        return commandHandler.execute(new CreateCommissionProductCommand(
                productId, commissioner, commissionRate, commissionType.name()));
    }

    public void deleteById(Long id) {
        commandHandler.execute(new DeleteCommissionProductCommand(id));
    }

    public BigDecimal calculateCommission(Long productId, BigDecimal saleAmount) {
        return queryHandler.handle(new CalculateCommissionQuery(productId, saleAmount));
    }
}
