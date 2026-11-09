package com.github.mizo432.reg.commission.cqrs.handler;

import com.github.mizo432.reg.commission.cqrs.command.CreateCommissionProductCommand;
import com.github.mizo432.reg.commission.cqrs.command.DeleteCommissionProductCommand;
import com.github.mizo432.reg.commission.domain.CommissionProduct;
import com.github.mizo432.reg.commission.repository.CommissionRepository;
import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.product.repository.ProductRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 委託関連のコマンドを処理するハンドラー
 */
@Component
@Transactional
public class CommissionCommandHandler {

    private final CommissionRepository commissionRepository;
    private final ProductRepository productRepository;

    public CommissionCommandHandler(CommissionRepository commissionRepository,
                                    ProductRepository productRepository) {
        this.commissionRepository = commissionRepository;
        this.productRepository = productRepository;
    }

    public CommissionProduct execute(CreateCommissionProductCommand command) {
        validateCommissioner(command.commissioner());
        validateCommissionRate(command.commissionRate());
        validateCommissionType(command.commissionType());

        var product = productRepository.findById(command.productId())
                .orElseThrow(() -> new IllegalArgumentException("商品が見つかりません: " + command.productId()));

        CommissionProduct commissionProduct = new CommissionProduct();
        commissionProduct.setProduct(product);
        commissionProduct.setCommissioner(command.commissioner());
        commissionProduct.setCommissionRate(command.commissionRate());
        commissionProduct.setCommissionType(CommissionProduct.CommissionType.valueOf(command.commissionType()));
        return commissionRepository.save(commissionProduct);
    }

    public Void execute(DeleteCommissionProductCommand command) {
        commissionRepository.deleteById(command.id());
        return null;
    }

    private void validateCommissioner(String commissioner) {
        if (commissioner == null || !StringUtils.hasText(commissioner)) {
            throw new ValidationException("commissioner", "委託者は空であってはいけません");
        }
    }

    private void validateCommissionRate(BigDecimal commissionRate) {
        if (commissionRate == null) {
            throw new ValidationException("commissionRate", "手数料率はnullであってはいけません");
        }
        if (commissionRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("commissionRate", "手数料率は0以上である必要があります: " + commissionRate);
        }
        if (commissionRate.compareTo(BigDecimal.ONE) > 0) {
            throw new ValidationException("commissionRate", "手数料率は1.0（100%）以下である必要があります: " + commissionRate);
        }
    }

    private void validateCommissionType(String commissionType) {
        if (commissionType == null || !StringUtils.hasText(commissionType)) {
            throw new ValidationException("commissionType", "手数料タイプは空であってはいけません");
        }
        try {
            CommissionProduct.CommissionType.valueOf(commissionType);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("commissionType", "無効な手数料タイプです: " + commissionType);
        }
    }
}
