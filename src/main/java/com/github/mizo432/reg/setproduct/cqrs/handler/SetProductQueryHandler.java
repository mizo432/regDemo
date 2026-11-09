package com.github.mizo432.reg.setproduct.cqrs.handler;

import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.product.domain.Product;
import com.github.mizo432.reg.product.domain.SetProduct;
import com.github.mizo432.reg.product.domain.SetProductItem;
import com.github.mizo432.reg.product.repository.SetProductRepository;
import com.github.mizo432.reg.product.service.ProductService;
import com.github.mizo432.reg.product.service.SetProductExpansion;
import com.github.mizo432.reg.setproduct.cqrs.query.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.github.mizo432.reg.product.service.SetProductExpansion.ExpandedItem;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * セット商品関連のクエリを処理するハンドラー
 */
@Component
@Transactional(readOnly = true)
public class SetProductQueryHandler {

    private final SetProductRepository setProductRepository;
    private final ProductService productService;

    public SetProductQueryHandler(SetProductRepository setProductRepository, ProductService productService) {
        this.setProductRepository = setProductRepository;
        this.productService = productService;
    }

    public List<SetProduct> handle(GetAllSetProductsQuery query) {
        return setProductRepository.findAll();
    }

    public List<SetProduct> handle(GetActiveSetProductsQuery query) {
        return setProductRepository.findByStatus(SetProduct.Status.ACTIVE);
    }

    public Optional<SetProduct> handle(GetSetProductByIdQuery query) {
        if (query.id() == null) {
            throw new ValidationException("id", "IDはnullであってはいけません");
        }
        return setProductRepository.findById(query.id());
    }

    public Optional<SetProductExpansion> handle(ExpandSetProductByBarcodeQuery query) {
        if (query.barcode() == null) {
            throw new ValidationException("barcode", "バーコードはnullであってはいけません");
        }
        SetProduct setProduct = setProductRepository.findBySetBarcode(query.barcode()).orElse(null);
        if (setProduct == null || setProduct.getStatus() != SetProduct.Status.ACTIVE) {
            return Optional.empty();
        }

        BigDecimal setPrice = setProduct.getSetPrice();

        List<SetProductItem> activeItems = new ArrayList<>();
        for (SetProductItem item : setProduct.getItems()) {
            Product product = item.getProduct();
            if (product.getStatus() == Product.Status.ACTIVE) {
                activeItems.add(item);
            }
        }

        if (activeItems.isEmpty()) {
            return Optional.empty();
        }

        List<BigDecimal> itemTotals = new ArrayList<>();
        BigDecimal itemsTotalPrice = BigDecimal.ZERO;

        for (SetProductItem item : activeItems) {
            Product product = item.getProduct();
            BigDecimal effectivePrice = productService.getEffectivePrice(product.getId())
                    .orElse(product.getPrice());
            BigDecimal itemTotal = effectivePrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            itemTotals.add(itemTotal);
            itemsTotalPrice = itemsTotalPrice.add(itemTotal);
        }

        List<BigDecimal> allocatedItemTotals = new ArrayList<>();
        BigDecimal allocatedTotal = BigDecimal.ZERO;
        int activeItemCount = activeItems.size();

        for (int i = 0; i < activeItemCount; i++) {
            BigDecimal itemTotal = itemTotals.get(i);

            BigDecimal allocatedItemTotal;
            if (itemsTotalPrice.compareTo(BigDecimal.ZERO) == 0) {
                allocatedItemTotal = BigDecimal.ZERO;
            } else {
                BigDecimal ratio = itemTotal.divide(itemsTotalPrice, 10, java.math.RoundingMode.HALF_UP);
                allocatedItemTotal = setPrice.multiply(ratio)
                        .setScale(2, java.math.RoundingMode.HALF_UP);
            }

            if (i == activeItemCount - 1) {
                allocatedItemTotal = setPrice.subtract(allocatedTotal);
            } else {
                allocatedTotal = allocatedTotal.add(allocatedItemTotal);
            }

            allocatedItemTotals.add(allocatedItemTotal);
        }

        List<ExpandedItem> items = new ArrayList<>();
        for (int i = 0; i < activeItemCount; i++) {
            SetProductItem item = activeItems.get(i);
            Product product = item.getProduct();

            BigDecimal allocatedPrice = allocatedItemTotals.get(i)
                    .divide(BigDecimal.valueOf(item.getQuantity()), 2, java.math.RoundingMode.HALF_UP);

            items.add(new ExpandedItem(
                    product.getId(),
                    product.getBarcode(),
                    product.getName(),
                    allocatedPrice,
                    item.getQuantity(),
                    product.getTaxCategory()
            ));
        }

        return Optional.of(new SetProductExpansion(
                setProduct.getSetName(),
                setProduct.getSetPrice(),
                items
        ));
    }
}
