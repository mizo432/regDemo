package com.github.mizo432.reg;

import com.github.mizo432.reg.commission.service.CommissionService;
import com.github.mizo432.reg.inventory.service.InventoryService;
import com.github.mizo432.reg.product.service.ProductService;
import com.github.mizo432.reg.product.service.SetProductService;
import com.github.mizo432.reg.sales.service.CartService;
import com.github.mizo432.reg.sales.service.SalesService;
import com.github.mizo432.reg.sync.service.SyncService;
import com.github.mizo432.reg.ui.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class CoolFxApplication extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        springContext = SpringApplication.run(CoolFxApplication.class);
    }

    @Override
    public void start(Stage stage) throws Exception {
        ProductService productService = springContext.getBean(ProductService.class);
        InventoryService inventoryService = springContext.getBean(InventoryService.class);
        SetProductService setProductService = springContext.getBean(SetProductService.class);
        CartService cartService = springContext.getBean(CartService.class);
        SalesService salesService = springContext.getBean(SalesService.class);
        CommissionService commissionService = springContext.getBean(CommissionService.class);
        SyncService syncService = springContext.getBean(SyncService.class);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        loader.setControllerFactory(c -> {
            try {
                // MainControllerのコンストラクタにSpring Beanを注入
                if (c == MainController.class) {
                    MainController controller = new MainController(
                            productService, inventoryService, setProductService,
                            cartService, salesService, commissionService, syncService);
                    controller.setPrimaryStage(stage);
                    return controller;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        });
        Parent root = loader.load();

        Scene scene = new Scene(root, 1024, 768);
        scene.getStylesheets().add(getClass().getResource("/css/material-design.css").toExternalForm());
        stage.setTitle("レジアプリ");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (springContext != null) {
            springContext.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
