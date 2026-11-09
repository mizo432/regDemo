package com.github.mizo432.reg.product.service;

import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.product.domain.Product;
import com.github.mizo432.reg.product.domain.TimeSale;
import com.github.mizo432.reg.product.repository.ProductRepository;
import com.github.mizo432.reg.product.repository.TimeSaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class TimeSaleService {

    private final TimeSaleRepository timeSaleRepository;
    private final ProductRepository productRepository;

    public TimeSaleService(TimeSaleRepository timeSaleRepository, ProductRepository productRepository) {
        this.timeSaleRepository = timeSaleRepository;
        this.productRepository = productRepository;
    }

    public List<TimeSale> findAll() {
        return timeSaleRepository.findAll();
    }

    public List<TimeSale> findByProductId(Long productId) {
        if (productId == null) {
            throw new ValidationException("productId", "商品IDはnullであってはいけません");
        }
        return timeSaleRepository.findByProductIdAndStatusOrderByStartDateDesc(productId, TimeSale.Status.ACTIVE);
    }

    public List<TimeSale> findActiveSales() {
        return timeSaleRepository.findByStatus(TimeSale.Status.ACTIVE);
    }

    @Transactional
    public TimeSale save(TimeSale timeSale) {
        // 入力検証
        if (timeSale == null) {
            throw new ValidationException("timeSale", "タイムセールはnullであってはいけません");
        }
        if (timeSale.getProduct() == null) {
            throw new ValidationException("product", "商品はnullであってはいけません");
        }
        if (timeSale.getSalePrice() == null) {
            throw new ValidationException("salePrice", "セール価格はnullであってはいけません");
        }
        if (timeSale.getSalePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("salePrice", "セール価格は0以上である必要があります: " + timeSale.getSalePrice());
        }
        if (timeSale.getStartDate() == null) {
            throw new ValidationException("startDate", "開始日はnullであってはいけません");
        }
        if (timeSale.getEndDate() == null) {
            throw new ValidationException("endDate", "終了日はnullであってはいけません");
        }
        if (!timeSale.getEndDate().isAfter(timeSale.getStartDate())) {
            throw new ValidationException("endDate", "終了日は開始日より後である必要があります");
        }
        return timeSaleRepository.save(timeSale);
    }

    @Transactional
    public TimeSale createTimeSale(Long productId, BigDecimal salePrice, LocalDateTime startDate, LocalDateTime endDate) {
        // 入力検証
        if (productId == null) {
            throw new ValidationException("productId", "商品IDはnullであってはいけません");
        }
        if (salePrice == null) {
            throw new ValidationException("salePrice", "セール価格はnullであってはいけません");
        }
        if (salePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("salePrice", "セール価格は0以上である必要があります: " + salePrice);
        }
        if (startDate == null) {
            throw new ValidationException("startDate", "開始日はnullであってはいけません");
        }
        if (endDate == null) {
            throw new ValidationException("endDate", "終了日はnullであってはいけません");
        }
        if (!endDate.isAfter(startDate)) {
            throw new ValidationException("endDate", "終了日は開始日より後である必要があります");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品が見つかりません: " + productId));
        TimeSale timeSale = new TimeSale();
        timeSale.setProduct(product);
        timeSale.setSalePrice(salePrice);
        timeSale.setStartDate(startDate);
        timeSale.setEndDate(endDate);
        return timeSaleRepository.save(timeSale);
    }

    @Transactional
    public void deleteById(Long id) {
        if (id == null) {
            throw new ValidationException("id", "IDはnullであってはいけません");
        }
        timeSaleRepository.deleteById(id);
    }
}
