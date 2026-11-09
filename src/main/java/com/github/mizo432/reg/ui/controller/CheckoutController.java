package com.github.mizo432.reg.ui.controller;

import com.github.mizo432.reg.product.domain.Product;
import com.github.mizo432.reg.product.service.ProductService;
import com.github.mizo432.reg.sales.domain.Transaction;
import com.github.mizo432.reg.sales.domain.TransactionItem;
import com.github.mizo432.reg.sales.service.CartService;
import com.github.mizo432.reg.sales.service.SalesService;
import com.github.mizo432.reg.ui.MessageSource;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;

import java.math.BigDecimal;

public class CheckoutController {

    @FXML
    private TextField barcodeField;

    @FXML
    private TextField searchField;

    @FXML
    private TableView<CartService.CartLine> cartTable;

    @FXML
    private TableColumn<CartService.CartLine, String> cartNameColumn;

    @FXML
    private TableColumn<CartService.CartLine, BigDecimal> cartPriceColumn;

    @FXML
    private TableColumn<CartService.CartLine, Integer> cartQuantityColumn;

    @FXML
    private TableColumn<CartService.CartLine, BigDecimal> cartTotalColumn;

    @FXML
    private Label subtotalLabel;

    @FXML
    private Label taxLabel;

    @FXML
    private Label totalLabel;

    @FXML
    private ComboBox<Transaction.PaymentMethod> paymentMethodCombo;

    @FXML
    private TextField paidAmountField;

    private final CartService cartService;
    private final SalesService salesService;
    private final ProductService productService;

    private Transaction currentCart;
    private final ObservableList<CartService.CartLine> cartLines = FXCollections.observableArrayList();

    public CheckoutController(CartService cartService, SalesService salesService, ProductService productService) {
        this.cartService = cartService;
        this.salesService = salesService;
        this.productService = productService;
    }

    @FXML
    public void initialize() {
        cartNameColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().name()));
        cartPriceColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().unitPrice()));
        cartQuantityColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().quantity()).asObject());
        cartTotalColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().total()));

        // 数量列を編集可能にする
        cartQuantityColumn.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.StringConverter<Integer>() {
            @Override
            public Integer fromString(String string) {
                if (string == null || string.trim().isEmpty()) {
                    return null;
                }
                try {
                    return Integer.parseInt(string.trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            }

            @Override
            public String toString(Integer integer) {
                return integer != null ? integer.toString() : "";
            }
        }));

        // 数量編集完了時のハンドラー
        cartQuantityColumn.setOnEditCommit(event -> {
            CartService.CartLine line = event.getRowValue();
            Integer newQuantity = event.getNewValue();
            if (newQuantity == null || newQuantity <= 0) {
                showWarning(MessageSource.getMessage("checkout.quantity-required"));
                updateCartDisplay();
                return;
            }
            try {
                cartService.updateQuantity(currentCart, line.productId(), line.unitPrice(), newQuantity);
                updateCartDisplay();
            } catch (IllegalArgumentException e) {
                showError(MessageSource.getMessage("checkout.quantity-invalid"));
                updateCartDisplay();
            }
        });

        cartTable.setItems(cartLines);

        paymentMethodCombo.setItems(FXCollections.observableArrayList(
                Transaction.PaymentMethod.CASH,
                Transaction.PaymentMethod.CARD,
                Transaction.PaymentMethod.VOUCHER,
                Transaction.PaymentMethod.OTHER
        ));
        paymentMethodCombo.setValue(Transaction.PaymentMethod.CASH);

        clearCart();
    }

    @FXML
    private void handleAddToCart() {
        String barcode = barcodeField.getText().trim();
        if (barcode.isEmpty()) {
            showWarning(MessageSource.getMessage("checkout.barcode-required"));
            return;
        }
        try {
            cartService.addToCart(currentCart, barcode, 1);
            barcodeField.clear();
            barcodeField.requestFocus();
            updateCartDisplay();
        } catch (IllegalArgumentException e) {
            showError(MessageSource.getMessage("checkout.product-not-found", barcode));
        }
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            return;
        }
        productService.findByBarcode(keyword).ifPresentOrElse(
                product -> {
                    try {
                        cartService.addToCart(currentCart, product.getBarcode(), 1);
                        updateCartDisplay();
                    } catch (IllegalArgumentException e) {
                        showError(MessageSource.getMessage("checkout.add-failed"));
                    }
                },
                () -> {
                    productService.findByNameContaining(keyword).stream().findFirst().ifPresent(product -> {
                        try {
                            cartService.addToCart(currentCart, product.getBarcode(), 1);
                            updateCartDisplay();
                        } catch (IllegalArgumentException e) {
                            showError(MessageSource.getMessage("checkout.add-failed"));
                        }
                    });
                }
        );
        searchField.clear();
    }

    @FXML
    private void handleRemoveFromCart() {
        CartService.CartLine selected = cartTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                // 単価も含めて特定し、同じ商品が異なる単価で複数の明細として存在する場合でも
                // 選択された明細のみを削除する
                cartService.removeFromCart(currentCart, selected.productId(), selected.unitPrice());
                updateCartDisplay();
            } catch (IllegalArgumentException e) {
                showError(MessageSource.getMessage("checkout.remove-failed", e.getMessage()));
            } catch (Exception e) {
                showError(MessageSource.getMessage("checkout.failed", e.getMessage()));
            }
        }
    }

    @FXML
    private void handleClearCart() {
        try {
            clearCart();
        } catch (IllegalArgumentException e) {
            showError(MessageSource.getMessage("checkout.clear-failed", e.getMessage()));
        } catch (Exception e) {
            showError(MessageSource.getMessage("checkout.failed", e.getMessage()));
        }
    }

    @FXML
    private void handleCancelCart() {
        try {
            clearCart();
        } catch (IllegalArgumentException e) {
            showError(MessageSource.getMessage("checkout.clear-failed", e.getMessage()));
        } catch (Exception e) {
            showError(MessageSource.getMessage("checkout.failed", e.getMessage()));
        }
    }

    @FXML
    private void handleCheckout() {
        if (currentCart.getItems().isEmpty()) {
            showWarning(MessageSource.getMessage("checkout.cart-empty"));
            return;
        }

        Transaction.PaymentMethod paymentMethod = paymentMethodCombo.getValue();
        if (paymentMethod == null) {
            showWarning(MessageSource.getMessage("checkout.payment-method-required"));
            return;
        }

        currentCart.setPaymentMethod(paymentMethod);

        BigDecimal paidAmount = null;
        if (!paidAmountField.getText().trim().isEmpty()) {
            try {
                paidAmount = new BigDecimal(paidAmountField.getText().trim());
                if (paidAmount.compareTo(BigDecimal.ZERO) < 0) {
                    showError(MessageSource.getMessage("checkout.paidAmount-negative"));
                    return;
                }
                currentCart.setPaidAmount(paidAmount);
            } catch (NumberFormatException e) {
                showError(MessageSource.getMessage("checkout.paidAmount-invalid"));
                return;
            }
        }

        try {
            Transaction completed = salesService.completeCheckout(currentCart);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(MessageSource.getMessage("checkout.complete.title"));
            alert.setHeaderText(MessageSource.getMessage("checkout.complete.header"));
            alert.setContentText(MessageSource.getMessage(
                    "checkout.complete.content",
                    completed.getTransactionId(),
                    completed.getGrandTotal()
            ));
            alert.showAndWait();

            clearCart();
        } catch (IllegalArgumentException e) {
            showError(MessageSource.getMessage("checkout.failed.argument", e.getMessage()));
        } catch (Exception e) {
            showError(MessageSource.getMessage("checkout.failed", e.getMessage()));
        }
    }

    private void clearCart() {
        // currentCartがnullの場合（初期化時など）は新規作成のみで戻る
        if (currentCart == null) {
            currentCart = cartService.createCart();
            updateCartDisplay();
            return;
        }
        // 旧カートの明細に残っていた予約在庫を解除する
        for (TransactionItem item : currentCart.getItems()) {
            try {
                cartService.releaseItemReservation(item.getProduct().getId(), item.getQuantity());
            } catch (Exception e) {
                // 予約解除の失敗はログ出力のみで、カートクリア自体は続行する
                java.util.logging.Logger.getLogger(CheckoutController.class.getName())
                        .warning("予約在庫の解除に失敗しました: 商品ID=" + item.getProduct().getId());
            }
        }
        currentCart = cartService.createCart();
        updateCartDisplay();
    }

    private void updateCartDisplay() {
        cartLines.setAll(cartService.getCartLines(currentCart));

        BigDecimal subtotal = cartService.calculateSubtotal(currentCart);
        BigDecimal tax = cartService.calculateTax(currentCart);
        BigDecimal total = cartService.calculateTotal(currentCart);

        subtotalLabel.setText(subtotal.toPlainString());
        taxLabel.setText(tax.toPlainString());
        totalLabel.setText(total.toPlainString());
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(MessageSource.getMessage("dialog.title.warning"));
        alert.setHeaderText(MessageSource.getMessage("dialog.header.warning"));
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(MessageSource.getMessage("dialog.title.error"));
        alert.setHeaderText(MessageSource.getMessage("dialog.header.error"));
        alert.setContentText(message);
        alert.showAndWait();
    }
}
