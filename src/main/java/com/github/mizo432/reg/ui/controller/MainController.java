package com.github.mizo432.reg.ui.controller;

import com.github.mizo432.reg.commission.service.CommissionService;
import com.github.mizo432.reg.inventory.service.InventoryService;
import com.github.mizo432.reg.product.service.ProductService;
import com.github.mizo432.reg.product.service.SetProductService;
import com.github.mizo432.reg.sales.service.CartService;
import com.github.mizo432.reg.sales.service.SalesService;
import com.github.mizo432.reg.sync.service.SyncService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class MainController {

    private final ProductService productService;
    private final InventoryService inventoryService;
    private final SetProductService setProductService;
    private final CartService cartService;
    private final SalesService salesService;
    private final CommissionService commissionService;
    private final SyncService syncService;

    private Stage primaryStage;

    @FXML
    private StackPane contentPane;

    public MainController(ProductService productService,
                          InventoryService inventoryService,
                          SetProductService setProductService,
                          CartService cartService,
                          SalesService salesService,
                          CommissionService commissionService,
                          SyncService syncService) {
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.setProductService = setProductService;
        this.cartService = cartService;
        this.salesService = salesService;
        this.commissionService = commissionService;
        this.syncService = syncService;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void setContentPane(StackPane contentPane) {
        this.contentPane = contentPane;
    }

    @FXML
    public void initialize() {
        if (contentPane != null) {
            handleCheckout();
        }
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            switch (fxmlPath) {
                case "/fxml/product.fxml" -> loader.setControllerFactory(c -> new ProductController(productService));
                case "/fxml/inventory.fxml" -> loader.setControllerFactory(c -> new InventoryController(inventoryService, productService));
                case "/fxml/set_product.fxml" -> loader.setControllerFactory(c -> new SetProductController(setProductService, productService));
                case "/fxml/checkout.fxml" -> loader.setControllerFactory(c -> new CheckoutController(cartService, salesService, productService));
                case "/fxml/transaction_history.fxml" -> loader.setControllerFactory(c -> new TransactionHistoryController(salesService));
                case "/fxml/commission.fxml" -> loader.setControllerFactory(c -> new CommissionController(commissionService, productService));
                case "/fxml/sync_status.fxml" -> loader.setControllerFactory(c -> new SyncStatusController(syncService));
            }
            Parent root = loader.load();
            contentPane.getChildren().setAll(root);
        } catch (IOException e) {
            throw new RuntimeException("画面の読み込みに失敗しました: " + fxmlPath, e);
        }
    }

    public void handleCheckout() {
        loadView("/fxml/checkout.fxml");
    }

    public void handleProduct() {
        loadView("/fxml/product.fxml");
    }

    public void handleInventory() {
        loadView("/fxml/inventory.fxml");
    }

    public void handleTransactionHistory() {
        loadView("/fxml/transaction_history.fxml");
    }

    public void handleCommission() {
        loadView("/fxml/commission.fxml");
    }

    public void handleSyncStatus() {
        loadView("/fxml/sync_status.fxml");
    }

    public void handleSetProduct() {
        loadView("/fxml/set_product.fxml");
    }
}
