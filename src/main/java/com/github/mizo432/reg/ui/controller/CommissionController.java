package com.github.mizo432.reg.ui.controller;

import com.github.mizo432.reg.commission.domain.CommissionProduct;
import com.github.mizo432.reg.commission.service.CommissionService;
import com.github.mizo432.reg.product.domain.Product;
import com.github.mizo432.reg.product.service.ProductService;
import com.github.mizo432.reg.ui.MessageSource;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.math.BigDecimal;

public class CommissionController {

    @FXML
    private TableView<CommissionProduct> commissionTable;

    @FXML
    private TableColumn<CommissionProduct, String> barcodeColumn;

    @FXML
    private TableColumn<CommissionProduct, String> nameColumn;

    @FXML
    private TableColumn<CommissionProduct, String> commissionerColumn;

    @FXML
    private TableColumn<CommissionProduct, String> rateColumn;

    @FXML
    private TableColumn<CommissionProduct, String> typeColumn;

    private final CommissionService commissionService;
    private final ProductService productService;
    private final ObservableList<CommissionProduct> commissionList = FXCollections.observableArrayList();

    public CommissionController(CommissionService commissionService, ProductService productService) {
        this.commissionService = commissionService;
        this.productService = productService;
    }

    @FXML
    public void initialize() {
        barcodeColumn.setCellValueFactory(cellData -> {
            CommissionProduct cp = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(cp.getProduct().getBarcode());
        });
        nameColumn.setCellValueFactory(cellData -> {
            CommissionProduct cp = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(cp.getProduct().getName());
        });
        commissionerColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCommissioner()));
        rateColumn.setCellValueFactory(cellData -> {
            CommissionProduct cp = cellData.getValue();
            BigDecimal rate = cp.getCommissionRate();
            return new javafx.beans.property.SimpleStringProperty(rate != null ? rate.toPlainString() : "");
        });
        typeColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getCommissionType().name()));

        commissionTable.setItems(commissionList);
        loadCommissionProducts();
    }

    private void loadCommissionProducts() {
        commissionList.setAll(commissionService.findActiveCommissionProducts());
    }

    @FXML
    private void handleNewCommission() {
        Dialog<CommissionProduct> dialog = new Dialog<>();
        dialog.setTitle(MessageSource.getMessage("commission.new.title"));
        dialog.setHeaderText(MessageSource.getMessage("commission.new.header"));

        ButtonType saveButtonType = new ButtonType(MessageSource.getMessage("commission.button.save"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField barcodeField = new TextField();
        barcodeField.setPromptText("バーコード");
        TextField commissionerField = new TextField();
        commissionerField.setPromptText("委託者");
        TextField rateField = new TextField();
        rateField.setPromptText("手数料率 (例: 0.1 = 10%)");

        grid.add(new Label("バーコード:"), 0, 0);
        grid.add(barcodeField, 1, 0);
        grid.add(new Label("委託者:"), 0, 1);
        grid.add(commissionerField, 1, 1);
        grid.add(new Label("手数料率:"), 0, 2);
        grid.add(rateField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String barcode = barcodeField.getText().trim();
                String commissioner = commissionerField.getText().trim();
                String rateText = rateField.getText().trim();

                if (barcode.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(MessageSource.getMessage("dialog.title.input-error"));
                    alert.setHeaderText(MessageSource.getMessage("dialog.header.input-error"));
                    alert.setContentText(MessageSource.getMessage("commission.barcode-required"));
                    alert.showAndWait();
                    return null;
                }
                if (commissioner.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(MessageSource.getMessage("dialog.title.input-error"));
                    alert.setHeaderText(MessageSource.getMessage("dialog.header.input-error"));
                    alert.setContentText(MessageSource.getMessage("commission.commissioner-required"));
                    alert.showAndWait();
                    return null;
                }
                if (rateText.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(MessageSource.getMessage("dialog.title.input-error"));
                    alert.setHeaderText(MessageSource.getMessage("dialog.header.input-error"));
                    alert.setContentText(MessageSource.getMessage("commission.rate-required"));
                    alert.showAndWait();
                    return null;
                }

                Product product = productService.findByBarcode(barcode)
                        .orElseThrow(() -> new IllegalArgumentException(MessageSource.getMessage("commission.product-not-found", barcode)));
                try {
                    BigDecimal rate = new BigDecimal(rateText);
                    if (rate.compareTo(BigDecimal.ZERO) < 0) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle(MessageSource.getMessage("dialog.title.input-error"));
                        alert.setHeaderText(MessageSource.getMessage("dialog.header.input-error"));
                        alert.setContentText(MessageSource.getMessage("commission.rate-negative"));
                        alert.showAndWait();
                        return null;
                    }
                    if (rate.compareTo(BigDecimal.ONE) > 0) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle(MessageSource.getMessage("dialog.title.input-error"));
                        alert.setHeaderText(MessageSource.getMessage("dialog.header.input-error"));
                        alert.setContentText(MessageSource.getMessage("commission.rate-exceeds-100"));
                        alert.showAndWait();
                        return null;
                    }
                    CommissionProduct cp = new CommissionProduct();
                    cp.setProduct(product);
                    cp.setCommissioner(commissioner);
                    cp.setCommissionRate(rate);
                    cp.setCommissionType(CommissionProduct.CommissionType.RATE);
                    return cp;
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(MessageSource.getMessage("dialog.title.input-error"));
                    alert.setHeaderText(MessageSource.getMessage("dialog.header.input-error"));
                    alert.setContentText(MessageSource.getMessage("commission.rate-invalid"));
                    alert.showAndWait();
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(cp -> {
            commissionService.save(cp);
            loadCommissionProducts();
        });
    }

    @FXML
    private void handleRefresh() {
        loadCommissionProducts();
    }
}
