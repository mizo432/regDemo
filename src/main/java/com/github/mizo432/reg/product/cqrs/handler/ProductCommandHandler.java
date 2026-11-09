package com.github.mizo432.reg.product.cqrs.handler;

import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.product.cqrs.command.CreateProductCommand;
import com.github.mizo432.reg.product.cqrs.command.DeleteProductCommand;
import com.github.mizo432.reg.product.cqrs.command.UpdateProductCommand;
import com.github.mizo432.reg.product.domain.Product;
import com.github.mizo432.reg.product.repository.ProductRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 商品関連のコマンドを処理するハンドラー
 */
@Component
@Transactional
public class ProductCommandHandler {

    private final ProductRepository productRepository;

    public ProductCommandHandler(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product execute(CreateProductCommand command) {
        validateProductName(command.name());
        validateBarcode(command.barcode());
        validatePrice(command.price());
        validateTaxCategory(command.taxCategory());

        Product product = new Product();
        product.setName(command.name());
        product.setBarcode(command.barcode());
        product.setPrice(command.price());
        product.setTaxCategory(Product.TaxCategory.valueOf(command.taxCategory()));
        product.setStatus(Product.Status.ACTIVE);
        return productRepository.save(product);
    }

    public Product execute(UpdateProductCommand command) {
        Product product = productRepository.findById(command.id())
                .orElseThrow(() -> new IllegalArgumentException("商品が見つかりません: " + command.id()));
        validateProductName(command.name());
        validateBarcode(command.barcode());
        validatePrice(command.price());
        validateTaxCategory(command.taxCategory());
        validateStatus(command.status());

        product.setName(command.name());
        product.setBarcode(command.barcode());
        product.setPrice(command.price());
        product.setTaxCategory(Product.TaxCategory.valueOf(command.taxCategory()));
        product.setStatus(Product.Status.valueOf(command.status()));
        return productRepository.save(product);
    }

    private void validateProductName(String name) {
        if (name == null || !StringUtils.hasText(name)) {
            throw new ValidationException("name", "商品名は空であってはいけません");
        }
    }

    private void validateBarcode(String barcode) {
        if (barcode == null || !StringUtils.hasText(barcode)) {
            throw new ValidationException("barcode", "バーコードは空であってはいけません");
        }
    }

    private void validatePrice(BigDecimal price) {
        if (price == null) {
            throw new ValidationException("price", "価格はnullであってはいけません");
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("price", "価格は0以上である必要があります: " + price);
        }
    }

    private void validateTaxCategory(String taxCategory) {
        if (taxCategory == null || !StringUtils.hasText(taxCategory)) {
            throw new ValidationException("taxCategory", "税種は空であってはいけません");
        }
        try {
            Product.TaxCategory.valueOf(taxCategory);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("taxCategory", "無効な税種です: " + taxCategory);
        }
    }

    private void validateStatus(String status) {
        if (status == null || !StringUtils.hasText(status)) {
            throw new ValidationException("status", "ステータスは空であってはいけません");
        }
        try {
            Product.Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("status", "無効なステータスです: " + status);
        }
    }

    public Void execute(DeleteProductCommand command) {
        productRepository.deleteById(command.id());
        return null;
    }
}
