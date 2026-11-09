package com.github.mizo432.reg.setproduct.cqrs.handler;

import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.product.domain.Product;
import com.github.mizo432.reg.product.domain.SetProduct;
import com.github.mizo432.reg.product.domain.SetProductItem;
import com.github.mizo432.reg.product.repository.ProductRepository;
import com.github.mizo432.reg.product.repository.SetProductRepository;
import com.github.mizo432.reg.setproduct.cqrs.command.CreateSetProductCommand;
import com.github.mizo432.reg.setproduct.cqrs.command.DeleteSetProductCommand;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * セット商品関連のコマンドを処理するハンドラー
 */
@Component
@Transactional
public class SetProductCommandHandler {

    private final SetProductRepository setProductRepository;
    private final ProductRepository productRepository;

    public SetProductCommandHandler(SetProductRepository setProductRepository,
                                    ProductRepository productRepository) {
        this.setProductRepository = setProductRepository;
        this.productRepository = productRepository;
    }

    public SetProduct execute(CreateSetProductCommand command) {
        // 入力検証
        validateSetBarcode(command.setBarcode());
        validateSetName(command.setName());
        validateSetPrice(command.setPrice());
        validateItems(command.items());

        SetProduct setProduct = new SetProduct();
        setProduct.setSetBarcode(command.setBarcode());
        setProduct.setSetName(command.setName());
        setProduct.setSetPrice(command.setPrice());
        setProduct.setStatus(SetProduct.Status.ACTIVE);

        for (CreateSetProductCommand.SetProductItemCommand itemCmd : command.items()) {
            if (itemCmd.productId() == null) {
                throw new ValidationException("productId", "商品IDはnullであってはいけません");
            }
            if (itemCmd.quantity() <= 0) {
                throw new ValidationException("quantity", "数量は1以上である必要があります: " + itemCmd.quantity());
            }
            Product product = productRepository.findById(itemCmd.productId())
                    .orElseThrow(() -> new IllegalArgumentException("商品が見つかりません: " + itemCmd.productId()));

            SetProductItem item = new SetProductItem();
            item.setProduct(product);
            item.setQuantity(itemCmd.quantity());
            setProduct.addItem(item);
        }

        return setProductRepository.save(setProduct);
    }

    public Void execute(DeleteSetProductCommand command) {
        if (command.id() == null) {
            throw new ValidationException("id", "セット商品IDはnullであってはいけません");
        }
        setProductRepository.deleteById(command.id());
        return null;
    }

    private void validateSetBarcode(String setBarcode) {
        if (setBarcode == null || !StringUtils.hasText(setBarcode)) {
            throw new ValidationException("setBarcode", "セットバーコードは空であってはいけません");
        }
    }

    private void validateSetName(String setName) {
        if (setName == null || !StringUtils.hasText(setName)) {
            throw new ValidationException("setName", "セット名は空であってはいけません");
        }
    }

    private void validateSetPrice(BigDecimal setPrice) {
        if (setPrice == null) {
            throw new ValidationException("setPrice", "セット価格はnullであってはいけません");
        }
        if (setPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("setPrice", "セット価格は0以上である必要があります: " + setPrice);
        }
    }

    private void validateItems(List<CreateSetProductCommand.SetProductItemCommand> items) {
        if (items == null || items.isEmpty()) {
            throw new ValidationException("items", "セット商品には1つ以上のアイテムが必要です");
        }
    }
}
