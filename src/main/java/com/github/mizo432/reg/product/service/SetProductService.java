package com.github.mizo432.reg.product.service;

import com.github.mizo432.reg.exception.ValidationException;
import com.github.mizo432.reg.product.domain.Product;
import com.github.mizo432.reg.product.domain.SetProduct;
import com.github.mizo432.reg.product.repository.ProductRepository;
import com.github.mizo432.reg.product.repository.SetProductRepository;
import com.github.mizo432.reg.setproduct.cqrs.command.CreateSetProductCommand;
import com.github.mizo432.reg.setproduct.cqrs.command.DeleteSetProductCommand;
import com.github.mizo432.reg.setproduct.cqrs.handler.SetProductCommandHandler;
import com.github.mizo432.reg.setproduct.cqrs.handler.SetProductQueryHandler;
import com.github.mizo432.reg.setproduct.cqrs.query.ExpandSetProductByBarcodeQuery;
import com.github.mizo432.reg.setproduct.cqrs.query.GetActiveSetProductsQuery;
import com.github.mizo432.reg.setproduct.cqrs.query.GetAllSetProductsQuery;
import com.github.mizo432.reg.setproduct.cqrs.query.GetSetProductByIdQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Collectors;

import java.util.List;
import java.util.Optional;

/**
 * セット商品サービスのファサード。
 * CQRSパターンを採用し、コマンド操作はSetProductCommandHandler、クエリ操作はSetProductQueryHandlerに委譲します。
 * 既存のAPIを維持するため、このファサードクラスを介してアクセスします。
 */
@Service
public class SetProductService {

    private final SetProductCommandHandler commandHandler;
    private final SetProductQueryHandler queryHandler;
    private final SetProductRepository setProductRepository;
    private final ProductRepository productRepository;

    public SetProductService(SetProductCommandHandler commandHandler,
                            SetProductQueryHandler queryHandler,
                            SetProductRepository setProductRepository,
                            ProductRepository productRepository) {
        this.commandHandler = commandHandler;
        this.queryHandler = queryHandler;
        this.setProductRepository = setProductRepository;
        this.productRepository = productRepository;
    }

    public List<SetProduct> findAll() {
        return queryHandler.handle(new GetAllSetProductsQuery());
    }

    public List<SetProduct> findActiveSetProducts() {
        return queryHandler.handle(new GetActiveSetProductsQuery());
    }

    public Optional<SetProduct> findById(Long id) {
        return queryHandler.handle(new GetSetProductByIdQuery(id));
    }

    public Optional<SetProduct> findBySetBarcode(String setBarcode) {
        return setProductRepository.findBySetBarcode(setBarcode);
    }

    public SetProduct save(SetProduct setProduct) {
        // 入力検証
        if (setProduct == null) {
            throw new ValidationException("setProduct", "セット商品はnullであってはいけません");
        }
        if (setProduct.getSetBarcode() == null || !StringUtils.hasText(setProduct.getSetBarcode())) {
            throw new ValidationException("setBarcode", "セットバーコードは空であってはいけません");
        }
        if (setProduct.getSetName() == null || !StringUtils.hasText(setProduct.getSetName())) {
            throw new ValidationException("setName", "セット名は空であってはいけません");
        }
        if (setProduct.getSetPrice() == null) {
            throw new ValidationException("setPrice", "セット価格はnullであってはいけません");
        }
        if (setProduct.getSetPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("setPrice", "セット価格は0以上である必要があります: " + setProduct.getSetPrice());
        }
        if (setProduct.getItems() == null || setProduct.getItems().isEmpty()) {
            throw new ValidationException("items", "セット商品には1つ以上のアイテムが必要です");
        }
        return setProductRepository.save(setProduct);
    }

    /**
     * セット商品を登録する（CQRSコマンド使用）。
     * 商品IDのリストからセット商品を作成します。
     */
    public SetProduct createSetProduct(String setBarcode, String setName, BigDecimal setPrice,
                                       List<Long> productIds, List<Integer> quantities) {
        // 入力検証
        if (setBarcode == null || !StringUtils.hasText(setBarcode)) {
            throw new ValidationException("setBarcode", "セットバーコードは空であってはいけません");
        }
        if (setName == null || !StringUtils.hasText(setName)) {
            throw new ValidationException("setName", "セット名は空であってはいけません");
        }
        if (setPrice == null) {
            throw new ValidationException("setPrice", "セット価格はnullであってはいけません");
        }
        if (setPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("setPrice", "セット価格は0以上である必要があります: " + setPrice);
        }
        if (productIds == null || productIds.isEmpty()) {
            throw new ValidationException("productIds", "商品IDのリストは空であってはいけません");
        }
        List<CreateSetProductCommand.SetProductItemCommand> itemCommands = productIds.stream()
                .map(id -> new CreateSetProductCommand.SetProductItemCommand(
                        id,
                        quantities != null && quantities.size() > productIds.indexOf(id)
                                ? quantities.get(productIds.indexOf(id)) : 1))
                .collect(Collectors.toList());
        return commandHandler.execute(new CreateSetProductCommand(setBarcode, setName, setPrice, itemCommands));
    }

    public void deleteById(Long id) {
        if (id == null) {
            throw new ValidationException("id", "IDはnullであってはいけません");
        }
        commandHandler.execute(new DeleteSetProductCommand(id));
    }

    public Optional<SetProductExpansion> expandByBarcode(String setBarcode) {
        if (setBarcode == null || !StringUtils.hasText(setBarcode)) {
            throw new ValidationException("setBarcode", "セットバーコードは空であってはいけません");
        }
        return queryHandler.handle(new ExpandSetProductByBarcodeQuery(setBarcode));
    }
}
