package com.github.mizo432.reg.ui.controller;

import com.github.mizo432.reg.product.domain.Product;
import com.github.mizo432.reg.product.domain.SetProduct;
import com.github.mizo432.reg.product.domain.SetProductItem;
import com.github.mizo432.reg.product.service.ProductService;
import com.github.mizo432.reg.product.service.SetProductService;
import com.github.mizo432.reg.ui.MessageSource;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SetProductController {

    @FXML
    private TextField searchField;

    @FXML
    private TableView<SetProduct> setProductTable;

    @FXML
    private TableColumn<SetProduct, String> barcodeColumn;

    @FXML
    private TableColumn<SetProduct, String> nameColumn;

    @FXML
    private TableColumn<SetProduct, BigDecimal> priceColumn;

    @FXML
    private TableColumn<SetProduct, SetProduct.Status> statusColumn;

    private final SetProductService setProductService;
    private final ProductService productService;
    private final ObservableList<SetProduct> setProductList = FXCollections.observableArrayList();

    public SetProductController(SetProductService setProductService, ProductService productService) {
        this.setProductService = setProductService;
        this.productService = productService;
    }

    @FXML
    public void initialize() {
        barcodeColumn.setCellValueFactory(new PropertyValueFactory<>("setBarcode"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("setName"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("setPrice"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        setProductTable.setItems(setProductList);
        loadSetProducts();
    }

    private void loadSetProducts() {
        setProductList.setAll(setProductService.findActiveSetProducts());
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadSetProducts();
            return;
        }
        // バーコードで検索
        if (setProductService.findBySetBarcode(keyword).isPresent()) {
            setProductList.setAll(setProductService.findBySetBarcode(keyword).get());
            return;
        }
        // 名称で検索
        setProductList.setAll(setProductService.findActiveSetProducts().stream()
                .filter(sp -> sp.getSetName().contains(keyword))
                .toList());
    }

    @FXML
    private void handleShowAll() {
        searchField.clear();
        loadSetProducts();
    }

    @FXML
    private void handleNewSetProduct() {
        Dialog<SetProduct> dialog = new Dialog<>();
        dialog.setTitle(MessageSource.getMessage("setproduct.new.title"));
        dialog.setHeaderText(MessageSource.getMessage("setproduct.new.header"));

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

        // 内包商品の選択
        ListView<Product> productList = new ListView<>();
        productList.setItems(FXCollections.observableArrayList(productService.findActiveProducts()));
        productList.setPrefHeight(150);

        TextField quantityField = new TextField("1");
        quantityField.setPromptText("数量");
        Button addButton = new Button("内包商品を追加");
        TableView<SetProductItem> itemsTable = new TableView<>();

        TableColumn<SetProductItem, String> itemBarcodeCol = new TableColumn<>("バーコード");
        itemBarcodeCol.setCellValueFactory(new PropertyValueFactory<>("product"));
        TableColumn<SetProductItem, Integer> itemQuantityCol = new TableColumn<>("数量");
        itemQuantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        itemsTable.getColumns().addAll(itemBarcodeCol, itemQuantityCol);
        itemsTable.setPrefHeight(100);

        SetProduct newSetProduct = new SetProduct();
        ObservableList<SetProductItem> newItemList = FXCollections.observableArrayList();

        addButton.setOnAction(e -> {
            Product selected = productList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                try {
                    int qty = Integer.parseInt(quantityField.getText().trim());
                    if (qty > 0) {
                        SetProductItem item = new SetProductItem();
                        item.setProduct(selected);
                        item.setQuantity(qty);
                        newItemList.add(item);
                        itemsTable.setItems(newItemList);
                    }
                } catch (NumberFormatException ex) {
                    // 無視
                }
            }
        });

        grid.add(new Label("バーコード:"), 0, 0);
        grid.add(barcodeField, 1, 0);
        grid.add(new Label("名称:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("価格:"), 0, 2);
        grid.add(priceField, 1, 2);
        grid.add(new Label("内包商品:"), 0, 3);
        grid.add(productList, 1, 3);
        grid.add(new Label("数量:"), 0, 4);
        grid.add(quantityField, 1, 4);
        grid.add(addButton, 2, 4);
        grid.add(itemsTable, 0, 5, 3, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String barcode = barcodeField.getText().trim();
                String name = nameField.getText().trim();
                String priceText = priceField.getText().trim();

                if (barcode.isEmpty()) {
                    showError("バーコードを入力してください");
                    return null;
                }
                if (name.isEmpty()) {
                    showError("名称を入力してください");
                    return null;
                }
                try {
                    BigDecimal price = new BigDecimal(priceText);
                    if (price.compareTo(java.math.BigDecimal.ZERO) < 0) {
                        showError("価格は0以上である必要があります");
                        return null;
                    }
                    newSetProduct.setSetBarcode(barcode);
                    newSetProduct.setSetName(name);
                    newSetProduct.setSetPrice(price);
                    newSetProduct.setItems(new ArrayList<>(newItemList));
                    return newSetProduct;
                } catch (NumberFormatException e) {
                    showError("価格の形式が正しくありません");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(setProduct -> {
            setProductService.save(setProduct);
            loadSetProducts();
        });
    }

    @FXML
    private void handleEditSetProduct() {
        SetProduct selected = setProductTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("セット商品を選択してください");
            return;
        }

        Dialog<SetProduct> dialog = new Dialog<>();
        dialog.setTitle(MessageSource.getMessage("setproduct.edit.title"));
        dialog.setHeaderText(MessageSource.getMessage("setproduct.edit.header"));

        ButtonType saveButtonType = new ButtonType(MessageSource.getMessage("product.button.save"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField barcodeField = new TextField(selected.getSetBarcode());
        barcodeField.setEditable(false);
        TextField nameField = new TextField(selected.getSetName());
        TextField priceField = new TextField(selected.getSetPrice().toString());

        ComboBox<SetProduct.Status> statusCombo = new ComboBox<>();
        statusCombo.setItems(FXCollections.observableArrayList(SetProduct.Status.ACTIVE, SetProduct.Status.INACTIVE));
        statusCombo.setValue(selected.getStatus());

        grid.add(new Label("バーコード:"), 0, 0);
        grid.add(barcodeField, 1, 0);
        grid.add(new Label("名称:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("価格:"), 0, 2);
        grid.add(priceField, 1, 2);
        grid.add(new Label("状態:"), 0, 3);
        grid.add(statusCombo, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                selected.setSetName(nameField.getText().trim());
                try {
                    BigDecimal price = new BigDecimal(priceField.getText().trim());
                    if (price.compareTo(java.math.BigDecimal.ZERO) < 0) {
                        showError("価格は0以上である必要があります");
                        return null;
                    }
                    selected.setSetPrice(price);
                } catch (NumberFormatException e) {
                    showError("価格の形式が正しくありません");
                    return null;
                }
                selected.setStatus(statusCombo.getValue());
                return selected;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(setProduct -> {
            setProductService.save(setProduct);
            loadSetProducts();
        });
    }

    @FXML
    private void handleDeleteSetProduct() {
        SetProduct selected = setProductTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("セット商品を選択してください");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(MessageSource.getMessage("setproduct.delete.title"));
        confirm.setHeaderText(MessageSource.getMessage("setproduct.delete.header"));
        confirm.setContentText(MessageSource.getMessage("setproduct.delete.content", selected.getSetName()));

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                setProductService.deleteById(selected.getId());
                loadSetProducts();
            }
        });
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
