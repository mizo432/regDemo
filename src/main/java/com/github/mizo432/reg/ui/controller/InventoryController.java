package com.github.mizo432.reg.ui.controller;

import com.github.mizo432.reg.inventory.domain.Inventory;
import com.github.mizo432.reg.inventory.service.InventoryService;
import com.github.mizo432.reg.product.domain.Product;
import com.github.mizo432.reg.product.service.ProductService;
import com.github.mizo432.reg.ui.MessageSource;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

public class InventoryController {

    @FXML
    private TextField searchField;

    @FXML
    private TableView<Inventory> inventoryTable;

    @FXML
    private TableColumn<Inventory, String> barcodeColumn;

    @FXML
    private TableColumn<Inventory, String> nameColumn;

    @FXML
    private TableColumn<Inventory, Integer> quantityColumn;

    @FXML
    private TableColumn<Inventory, Integer> reservedColumn;

    @FXML
    private TableColumn<Inventory, Integer> availableColumn;

    private final InventoryService inventoryService;
    private final ProductService productService;
    private final ObservableList<Inventory> inventoryList = FXCollections.observableArrayList();

    public InventoryController(InventoryService inventoryService, ProductService productService) {
        this.inventoryService = inventoryService;
        this.productService = productService;
    }

    @FXML
    public void initialize() {
        barcodeColumn.setCellValueFactory(cellData -> {
            Inventory inv = cellData.getValue();
            Product product = inv.getProduct();
            return new javafx.beans.property.SimpleStringProperty(product.getBarcode());
        });
        nameColumn.setCellValueFactory(cellData -> {
            Inventory inv = cellData.getValue();
            Product product = inv.getProduct();
            return new javafx.beans.property.SimpleStringProperty(product.getName());
        });
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        reservedColumn.setCellValueFactory(new PropertyValueFactory<>("reservedQuantity"));
        availableColumn.setCellValueFactory(cellData -> {
            Inventory inv = cellData.getValue();
            return new javafx.beans.property.SimpleIntegerProperty(inv.getAvailableQuantity()).asObject();
        });

        inventoryTable.setItems(inventoryList);
        loadInventory();
    }

    private void loadInventory() {
        inventoryList.setAll(inventoryService.findAll());
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadInventory();
            return;
        }
        inventoryList.setAll(inventoryService.findAll().stream()
                .filter(inv -> inv.getProduct().getName().contains(keyword))
                .toList());
    }

    @FXML
    private void handleShowAll() {
        searchField.clear();
        loadInventory();
    }

    @FXML
    private void handleRefresh() {
        loadInventory();
    }

    @FXML
    private void handleEditInventory() {
        Inventory selected = inventoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning(MessageSource.getMessage("inventory.select-for-edit"));
            return;
        }

        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle(MessageSource.getMessage("inventory.edit.title"));
        dialog.setHeaderText(MessageSource.getMessage("inventory.edit.header", selected.getProduct().getName()));

        ButtonType saveButtonType = new ButtonType(MessageSource.getMessage("product.button.save"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        Label productNameLabel = new Label(selected.getProduct().getName());
        TextField quantityField = new TextField(String.valueOf(selected.getQuantity()));

        grid.add(new Label("商品:"), 0, 0);
        grid.add(productNameLabel, 1, 0);
        grid.add(new Label("在庫数:"), 0, 1);
        grid.add(quantityField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    String text = quantityField.getText().trim();
                    if (text.isEmpty()) {
                        throw new NumberFormatException();
                    }
                    int quantity = Integer.parseInt(text);
                    if (quantity < 0) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle(MessageSource.getMessage("dialog.title.input-error"));
                        alert.setHeaderText(MessageSource.getMessage("dialog.header.input-error"));
                        alert.setContentText(MessageSource.getMessage("inventory.quantity-negative"));
                        alert.showAndWait();
                        return null;
                    }
                    return quantity;
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(MessageSource.getMessage("dialog.title.input-error"));
                    alert.setHeaderText(MessageSource.getMessage("dialog.header.input-error"));
                    alert.setContentText(MessageSource.getMessage("inventory.quantity-invalid"));
                    alert.showAndWait();
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(quantity -> {
            inventoryService.createOrUpdateInventory(selected.getProduct().getId(), quantity);
            loadInventory();
            showInfo(MessageSource.getMessage("inventory.update.success"));
        });
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(MessageSource.getMessage("dialog.title.warning"));
        alert.setHeaderText(MessageSource.getMessage("dialog.header.warning"));
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(MessageSource.getMessage("dialog.title.info"));
        alert.setHeaderText(MessageSource.getMessage("dialog.header.info"));
        alert.setContentText(message);
        alert.showAndWait();
    }
}
