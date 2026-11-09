package com.github.mizo432.reg.product.cqrs.handler;

import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.product.cqrs.query.GetActiveProductsQuery;
import com.github.mizo432.reg.product.cqrs.query.GetAllProductsQuery;
import com.github.mizo432.reg.product.cqrs.query.GetEffectivePriceQuery;
import com.github.mizo432.reg.product.cqrs.query.GetProductByBarcodeQuery;
import com.github.mizo432.reg.product.cqrs.query.GetProductByIdQuery;
import com.github.mizo432.reg.product.cqrs.query.SearchProductsByNameQuery;
import com.github.mizo432.reg.product.domain.Product;
import com.github.mizo432.reg.product.domain.TimeSale;
import com.github.mizo432.reg.product.repository.ProductRepository;
import com.github.mizo432.reg.product.repository.TimeSaleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 商品関連のクエリを処理するハンドラー
 */
@Component
@Transactional(readOnly = true)
public class ProductQueryHandler {

    private final ProductRepository productRepository;
    private final TimeSaleRepository timeSaleRepository;

    public ProductQueryHandler(ProductRepository productRepository, TimeSaleRepository timeSaleRepository) {
        this.productRepository = productRepository;
        this.timeSaleRepository = timeSaleRepository;
    }

    public List<Product> handle(GetAllProductsQuery query) {
        return productRepository.findAll();
    }

    public Optional<Product> handle(GetProductByIdQuery query) {
        if (query.id() == null) {
            throw new ValidationException("id", "IDはnullであってはいけません");
        }
        return productRepository.findById(query.id());
    }

    public Optional<Product> handle(GetProductByBarcodeQuery query) {
        if (query.barcode() == null) {
            throw new ValidationException("barcode", "バーコードはnullであってはいけません");
        }
        return productRepository.findByBarcode(query.barcode());
    }

    public List<Product> handle(SearchProductsByNameQuery query) {
        if (query.name() == null) {
            throw new ValidationException("name", "商品名はnullであってはいけません");
        }
        return productRepository.findByNameContaining(query.name());
    }

    public List<Product> handle(GetActiveProductsQuery query) {
        return productRepository.findByStatus(Product.Status.ACTIVE);
    }

    public Optional<BigDecimal> handle(GetEffectivePriceQuery query) {
        if (query.productId() == null) {
            throw new ValidationException("productId", "商品IDはnullであってはいけません");
        }
        Optional<Product> product = productRepository.findById(query.productId());
        if (product.isEmpty()) {
            return Optional.empty();
        }
        List<TimeSale> activeSales = timeSaleRepository.findByProductIdAndStatusOrderByStartDateDesc(
                        query.productId(), TimeSale.Status.ACTIVE)
                .stream()
                .filter(TimeSale::isActiveNow)
                .toList();
        if (activeSales.isEmpty()) {
            return Optional.of(product.get().getPrice());
        }
        return Optional.of(activeSales.get(0).getSalePrice());
    }
}
