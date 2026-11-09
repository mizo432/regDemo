package com.github.mizo432.reg.product.service;

import com.github.mizo432.reg.product.cqrs.command.CreateProductCommand;
import com.github.mizo432.reg.product.cqrs.command.DeleteProductCommand;
import com.github.mizo432.reg.product.cqrs.command.UpdateProductCommand;
import com.github.mizo432.reg.product.cqrs.handler.ProductCommandHandler;
import com.github.mizo432.reg.product.cqrs.handler.ProductQueryHandler;
import com.github.mizo432.reg.product.cqrs.query.GetActiveProductsQuery;
import com.github.mizo432.reg.product.cqrs.query.GetAllProductsQuery;
import com.github.mizo432.reg.product.cqrs.query.GetEffectivePriceQuery;
import com.github.mizo432.reg.product.cqrs.query.GetProductByBarcodeQuery;
import com.github.mizo432.reg.product.cqrs.query.GetProductByIdQuery;
import com.github.mizo432.reg.product.cqrs.query.SearchProductsByNameQuery;
import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.product.domain.Product;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 商品サービスのファサード。
 * CQRSパターンを採用し、コマンド操作はProductCommandHandler、クエリ操作はProductQueryHandlerに委譲します。
 * 既存のAPIを維持するため、このファサードクラスを介してアクセスします。
 */
@Service
public class ProductService {

    private final ProductCommandHandler commandHandler;
    private final ProductQueryHandler queryHandler;

    public ProductService(ProductCommandHandler commandHandler, ProductQueryHandler queryHandler) {
        this.commandHandler = commandHandler;
        this.queryHandler = queryHandler;
    }

    public List<Product> findAll() {
        return queryHandler.handle(new GetAllProductsQuery());
    }

    public Optional<Product> findById(Long id) {
        if (id == null) {
            throw new ValidationException("id", "IDはnullであってはいけません");
        }
        return queryHandler.handle(new GetProductByIdQuery(id));
    }

    public Optional<Product> findByBarcode(String barcode) {
        if (barcode == null || !StringUtils.hasText(barcode)) {
            throw new ValidationException("barcode", "バーコードは空であってはいけません");
        }
        return queryHandler.handle(new GetProductByBarcodeQuery(barcode));
    }

    public List<Product> findByNameContaining(String name) {
        if (name == null || !StringUtils.hasText(name)) {
            throw new ValidationException("name", "商品名は空であってはいけません");
        }
        return queryHandler.handle(new SearchProductsByNameQuery(name));
    }

    public List<Product> findActiveProducts() {
        return queryHandler.handle(new GetActiveProductsQuery());
    }

    public Product save(Product product) {
        if (product == null) {
            throw new ValidationException("product", "商品はnullであってはいけません");
        }
        if (product.getId() == null) {
            return commandHandler.execute(new CreateProductCommand(
                    product.getName(),
                    product.getBarcode(),
                    product.getPrice(),
                    product.getTaxCategory().name()
            ));
        } else {
            return commandHandler.execute(new UpdateProductCommand(
                    product.getId(),
                    product.getName(),
                    product.getBarcode(),
                    product.getPrice(),
                    product.getTaxCategory().name(),
                    product.getStatus().name()
            ));
        }
    }

    public void deleteById(Long id) {
        if (id == null) {
            throw new ValidationException("id", "IDはnullであってはいけません");
        }
        commandHandler.execute(new DeleteProductCommand(id));
    }

    public Optional<BigDecimal> getEffectivePrice(Long productId) {
        if (productId == null) {
            throw new ValidationException("productId", "商品IDはnullであってはいけません");
        }
        return queryHandler.handle(new GetEffectivePriceQuery(productId));
    }
}
