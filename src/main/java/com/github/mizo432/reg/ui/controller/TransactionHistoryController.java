package com.github.mizo432.reg.ui.controller;

import com.github.mizo432.reg.sales.domain.Transaction;
import com.github.mizo432.reg.sales.service.SalesService;
import com.github.mizo432.reg.ui.MessageSource;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.format.DateTimeFormatter;

public class TransactionHistoryController {

    @FXML
    private ComboBox<String> statusCombo;

    @FXML
    private TableView<Transaction> transactionTable;

    @FXML
    private TableColumn<Transaction, String> transactionIdColumn;

    @FXML
    private TableColumn<Transaction, String> dateColumn;

    @FXML
    private TableColumn<Transaction, String> totalColumn;

    @FXML
    private TableColumn<Transaction, String> paymentColumn;

    @FXML
    private TableColumn<Transaction, String> statusColumn;

    private final SalesService salesService;
    private final ObservableList<Transaction> transactionList = FXCollections.observableArrayList();
    
    // オペレーターID（環境変数またはシステムプロパティから取得、デフォルトは"operator"）
    private static final String OPERATOR_ID = System.getProperty("reg.operatorId",
            System.getenv("REG_OPERATOR_ID") != null ? System.getenv("REG_OPERATOR_ID") : "operator");

    public TransactionHistoryController(SalesService salesService) {
        this.salesService = salesService;
    }

    @FXML
    public void initialize() {
        transactionIdColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTransactionId()));
        dateColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        totalColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getGrandTotal().toPlainString()));
        paymentColumn.setCellValueFactory(cellData -> {
            Transaction.PaymentMethod method = cellData.getValue().getPaymentMethod();
            return new javafx.beans.property.SimpleStringProperty(method != null ? method.name() : "");
        });
        statusColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus().name()));

        transactionTable.setItems(transactionList);

        statusCombo.setItems(FXCollections.observableArrayList("全件", "COMPLETED", "CANCELLED", "RETURNED"));
        statusCombo.setValue("全件");
        statusCombo.valueProperty().addListener((obs, old, newVal) -> loadTransactions());

        loadTransactions();
    }

    private void loadTransactions() {
        String selected = statusCombo.getValue();
        if ("全件".equals(selected)) {
            transactionList.setAll(salesService.findAll());
        } else {
            Transaction.Status status = Transaction.Status.valueOf(selected);
            transactionList.setAll(salesService.findByStatus(status));
        }
    }

    @FXML
    private void handleRefresh() {
        loadTransactions();
    }

    @FXML
    private void handleCancel() {
        Transaction selected = transactionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning(MessageSource.getMessage("transaction.select-for-cancel"));
            return;
        }
        if (selected.getStatus() != Transaction.Status.COMPLETED) {
            showWarning(MessageSource.getMessage("transaction.status-completed-only"));
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(MessageSource.getMessage("transaction.cancel.title"));
        dialog.setHeaderText(MessageSource.getMessage("transaction.cancel.header"));

        ButtonType cancelButtonType = new ButtonType(MessageSource.getMessage("transaction.button.cancel"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(cancelButtonType, ButtonType.CANCEL);

        TextArea reasonField = new TextArea();
        reasonField.setPromptText("取消理由");
        reasonField.setPrefRowCount(3);
        dialog.getDialogPane().setContent(reasonField);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == cancelButtonType) {
                return reasonField.getText().trim();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(reason -> {
            try {
                salesService.cancelTransaction(selected.getTransactionId(), reason, OPERATOR_ID);
                loadTransactions();
                showInfo(MessageSource.getMessage("transaction.cancel.success"));
            } catch (IllegalArgumentException e) {
                showError(MessageSource.getMessage("transaction.cancel.failed.argument", e.getMessage()));
            } catch (Exception e) {
                showError(MessageSource.getMessage("transaction.cancel.failed", e.getMessage()));
            }
        });
    }

    @FXML
    private void handleReturn() {
        Transaction selected = transactionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning(MessageSource.getMessage("transaction.select-for-return"));
            return;
        }
        if (selected.getStatus() != Transaction.Status.COMPLETED) {
            showWarning(MessageSource.getMessage("transaction.status-completed-only-return"));
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(MessageSource.getMessage("transaction.return.title"));
        dialog.setHeaderText(MessageSource.getMessage("transaction.return.header"));

        ButtonType returnButtonType = new ButtonType(MessageSource.getMessage("transaction.button.return"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(returnButtonType, ButtonType.CANCEL);

        TextArea reasonField = new TextArea();
        reasonField.setPromptText("返品理由");
        reasonField.setPrefRowCount(3);
        dialog.getDialogPane().setContent(reasonField);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == returnButtonType) {
                return reasonField.getText().trim();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(reason -> {
            try {
                salesService.returnTransaction(selected.getTransactionId(), reason, OPERATOR_ID);
                loadTransactions();
                showInfo(MessageSource.getMessage("transaction.return.success"));
            } catch (IllegalArgumentException e) {
                showError(MessageSource.getMessage("transaction.return.failed.argument", e.getMessage()));
            } catch (Exception e) {
                showError(MessageSource.getMessage("transaction.return.failed", e.getMessage()));
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

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(MessageSource.getMessage("dialog.title.info"));
        alert.setHeaderText(MessageSource.getMessage("dialog.header.info"));
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
