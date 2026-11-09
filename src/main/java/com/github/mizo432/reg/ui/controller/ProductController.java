package com.github.mizo432.reg.ui.controller;

import com.github.mizo432.reg.product.domain.Product;
import com.github.mizo432.reg.product.service.ProductService;
import com.github.mizo432.reg.ui.MessageSource;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.math.BigDecimal;

public class ProductController {

    @FXML
    private TextField searchField;

    @FXML
    private TableView<Product> productTable;

    @FXML
    private TableColumn<Product, String> barcodeColumn;

    @FXML
    private TableColumn<Product, String> nameColumn;

    @FXML
    private TableColumn<Product, BigDecimal> priceColumn;

    @FXML
    private TableColumn<Product, Product.TaxCategory> taxCategoryColumn;

    @FXML
    private TableColumn<Product, Product.Status> statusColumn;

    private final ProductService productService;
    private final ObservableList<Product> productList = FXCollections.observableArrayList();

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @FXML
    public void initialize() {
        barcodeColumn.setCellValueFactory(new PropertyValueFactory<>("barcode"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        taxCategoryColumn.setCellValueFactory(new PropertyValueFactory<>("taxCategory"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        productTable.setItems(productList);
        loadProducts();
    }

    private void loadProducts() {
        productList.setAll(productService.findActiveProducts());
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadProducts();
            return;
        }
        // バーコードで検索
        if (productService.findByBarcode(keyword).isPresent()) {
            productList.setAll(productService.findByBarcode(keyword).get());
            return;
        }
        // 名称で検索
        productList.setAll(productService.findByNameContaining(keyword));
    }

    @FXML
    private void handleShowAll() {
        searchField.clear();
        loadProducts();
    }

    @FXML
    private void handleNewProduct() {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle(MessageSource.getMessage("product.new.title"));
        dialog.setHeaderText(MessageSource.getMessage("product.new.header"));

        ButtonType saveButtonType = new ButtonType(MessageSource.getMessage("product.button.save"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField barcodeField = new TextField();
        barcodeField.setPromptText("バーコード");
        TextField nameField = new TextField();
        nameField.setPromptText("名称");
        TextField priceField = new TextField();
        priceField.setPromptText("価格");

        grid.add(new Label("バーコード:"), 0, 0);
        grid.add(barcodeField, 1, 0);
        grid.add(new Label("名称:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("価格:"), 0, 2);
        grid.add(priceField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String barcode = barcodeField.getText().trim();
                String name = nameField.getText().trim();
                String priceText = priceField.getText().trim();

                if (barcode.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(MessageSource.getMessage("dialog.title.input-error"));
                    alert.setHeaderText(MessageSource.getMessage("dialog.header.input-error"));
                    alert.setContentText(MessageSource.getMessage("product.barcode-required"));
                    alert.showAndWait();
                    return null;
                }
                if (name.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(MessageSource.getMessage("dialog.title.input-error"));
                    alert.setHeaderText(MessageSource.getMessage("dialog.header.input-error"));
                    alert.setContentText(MessageSource.getMessage("product.name-required"));
                    alert.showAndWait();
                    return null;
                }
                try {
                    BigDecimal price = new BigDecimal(priceText);
                    if (price.compareTo(BigDecimal.ZERO) < 0) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle(MessageSource.getMessage("dialog.title.input-error"));
                        alert.setHeaderText(MessageSource.getMessage("dialog.header.input-error"));
                        alert.setContentText(MessageSource.getMessage("product.price-negative"));
                        alert.showAndWait();
                        return null;
                    }
                    Product product = new Product();
                    product.setBarcode(barcode);
                    product.setName(name);
                    product.setPrice(price);
                    return product;
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(MessageSource.getMessage("dialog.title.input-error"));
                    alert.setHeaderText(MessageSource.getMessage("dialog.header.input-error"));
                    alert.setContentText(MessageSource.getMessage("product.price-invalid"));
                    alert.showAndWait();
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(product -> {
            productService.save(product);
            loadProducts();
        });
    }

    @FXML
    private void handleEditProduct() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(MessageSource.getMessage("dialog.title.selection-error"));
            alert.setHeaderText(MessageSource.getMessage("dialog.header.selection-error"));
            alert.setContentText(MessageSource.getMessage("product.select-for-edit"));
            alert.showAndWait();
            return;
        }

        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle(MessageSource.getMessage("product.edit.title"));
        dialog.setHeaderText(MessageSource.getMessage("product.edit.header"));

        ButtonType saveButtonType = new ButtonType(MessageSource.getMessage("product.button.save"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField barcodeField = new TextField(selected.getBarcode());
        barcodeField.setEditable(false);
        TextField nameField = new TextField(selected.getName());
        TextField priceField = new TextField(selected.getPrice().toString());

        grid.add(new Label("バーコード:"), 0, 0);
        grid.add(barcodeField, 1, 0);
        grid.add(new Label("名称:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("価格:"), 0, 2);
        grid.add(priceField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                selected.setName(nameField.getText().trim());
                try {
                    BigDecimal price = new BigDecimal(priceField.getText().trim());
                    if (price.compareTo(BigDecimal.ZERO) < 0) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle(MessageSource.getMessage("dialog.title.input-error"));
                        alert.setHeaderText(MessageSource.getMessage("dialog.header.input-error"));
                        alert.setContentText(MessageSource.getMessage("product.price-negative"));
                        alert.showAndWait();
                        return null;
                    }
                    selected.setPrice(price);
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(MessageSource.getMessage("dialog.title.input-error"));
                    alert.setHeaderText(MessageSource.getMessage("dialog.header.input-error"));
                    alert.setContentText(MessageSource.getMessage("product.price-invalid"));
                    alert.showAndWait();
                    return null;
                }
                return selected;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(product -> {
            productService.save(product);
            loadProducts();
        });
    }

    @FXML
    private void handleDeleteProduct() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(MessageSource.getMessage("dialog.title.selection-error"));
            alert.setHeaderText(MessageSource.getMessage("dialog.header.selection-error"));
            alert.setContentText(MessageSource.getMessage("product.select-for-delete"));
            alert.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(MessageSource.getMessage("product.delete.title"));
        confirm.setHeaderText(MessageSource.getMessage("product.delete.header"));
        confirm.setContentText(MessageSource.getMessage("product.delete.content", selected.getName()));

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                productService.deleteById(selected.getId());
                loadProducts();
            }
        });
    }
}
