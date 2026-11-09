package com.github.mizo432.reg.ui.controller;

import com.github.mizo432.reg.sync.domain.SyncInbox;
import com.github.mizo432.reg.sync.domain.SyncOutbox;
import com.github.mizo432.reg.sync.service.SyncService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class SyncStatusController {

    @FXML
    private TableView<SyncOutbox> outboxTable;

    @FXML
    private TableColumn<SyncOutbox, String> outboxTransactionColumn;

    @FXML
    private TableColumn<SyncOutbox, String> outboxStatusColumn;

    @FXML
    private TableColumn<SyncOutbox, Integer> outboxRetryColumn;

    @FXML
    private TableColumn<SyncOutbox, String> outboxErrorColumn;

    @FXML
    private TableView<SyncInbox> inboxTable;

    @FXML
    private TableColumn<SyncInbox, String> inboxTypeColumn;

    @FXML
    private TableColumn<SyncInbox, String> inboxIdColumn;

    @FXML
    private TableColumn<SyncInbox, String> inboxAppliedColumn;

    private final SyncService syncService;
    private final ObservableList<SyncOutbox> outboxList = FXCollections.observableArrayList();
    private final ObservableList<SyncInbox> inboxList = FXCollections.observableArrayList();

    public SyncStatusController(SyncService syncService) {
        this.syncService = syncService;
    }

    @FXML
    public void initialize() {
        outboxTransactionColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTransactionId()));
        outboxStatusColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus().name()));
        outboxRetryColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getRetryCount()).asObject());
        outboxErrorColumn.setCellValueFactory(cellData -> {
            String error = cellData.getValue().getLastError();
            return new javafx.beans.property.SimpleStringProperty(error != null ? error : "");
        });

        inboxTypeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEventType()));
        inboxIdColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEventId()));
        inboxAppliedColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().isApplied() ? "はい" : "いいえ"));

        outboxTable.setItems(outboxList);
        inboxTable.setItems(inboxList);

        loadSyncStatus();
    }

    private void loadSyncStatus() {
        outboxList.setAll(syncService.findPendingOutbox());
        inboxList.setAll(syncService.findUnappliedInbox());
    }

    @FXML
    private void handleRefresh() {
        loadSyncStatus();
    }
}
