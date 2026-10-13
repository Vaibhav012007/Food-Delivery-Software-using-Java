package com.app.desktop;

import com.app.dao.DeliveryBatchDAO;
import com.app.dao_implementation.DeliveryBatchDAOImpl;
import com.app.models.DeliveryBatch;
import com.app.models.Order;
import com.app.service.DeliveryBatchingService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class DeliveryBatchController {

    @FXML private TableView<DeliveryBatch> batchTable;
    @FXML private TableColumn<DeliveryBatch, Number> idCol;
    @FXML private TableColumn<DeliveryBatch, Number> restaurantCol;
    @FXML private TableColumn<DeliveryBatch, String> statusCol;
    @FXML private TableColumn<DeliveryBatch, Number> maxSizeCol;
    @FXML private TableColumn<DeliveryBatch, String> createdCol;
    @FXML private TableColumn<DeliveryBatch, String> dispatchedCol;

    @FXML private Label selectedBatchLabel;
    @FXML private Button dispatchButton;
    @FXML private ListView<String> batchOrdersList;
    @FXML private Label statusMessage;

    private final DeliveryBatchDAO batchDAO = new DeliveryBatchDAOImpl();
    private final DeliveryBatchingService batchingService = new DeliveryBatchingService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, hh:mm a");

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getBatchId()));
        restaurantCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getRestaurantId()));
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        maxSizeCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getMaxSize()));
        createdCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCreatedAt() != null ? dateFormat.format(data.getValue().getCreatedAt()) : "-"));
        dispatchedCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDispatchedAt() != null ? dateFormat.format(data.getValue().getDispatchedAt()) : "-"));

        batchTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showBatchDetails(newVal);
            }
        });

        loadBatches();
    }

    @FXML
    public void loadBatches() {
        try {
            List<DeliveryBatch> batches = batchDAO.getAllBatches(100);
            batchTable.setItems(FXCollections.observableArrayList(batches));
        } catch (Exception e) {
            showMsg("Failed to load batches: " + e.getMessage(), true);
        }
    }

    private void showBatchDetails(DeliveryBatch batch) {
        selectedBatchLabel.setText("Batch #" + batch.getBatchId() + " (" + batch.getStatus().toUpperCase() + ")");
        dispatchButton.setDisable(!"open".equalsIgnoreCase(batch.getStatus()));

        try {
            List<Order> orders = batchDAO.getOrdersInBatch(batch.getBatchId());
            List<String> items = new ArrayList<>();
            for (Order o : orders) {
                items.add("Order #" + o.getOrderId() + " - ₹" + o.getTotalAmount() + " (" + o.getStatus() + ")");
            }
            batchOrdersList.setItems(FXCollections.observableArrayList(items));
        } catch (Exception e) {
            batchOrdersList.setItems(FXCollections.observableArrayList("Error loading batch orders"));
        }
    }

    @FXML
    private void handleRunBatching() {
        try {
            int batchedCount = batchingService.runBatchingCycle();
            showMsg("Batching cycle completed! Orders assigned: " + batchedCount, false);
            loadBatches();
        } catch (Exception e) {
            showMsg("Batching error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleDispatch() {
        DeliveryBatch selected = batchTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            batchingService.dispatchBatch(selected.getBatchId());
            showMsg("Batch #" + selected.getBatchId() + " dispatched successfully!", false);
            loadBatches();
        } catch (Exception e) {
            showMsg("Failed to dispatch: " + e.getMessage(), true);
        }
    }

    private void showMsg(String msg, boolean isError) {
        statusMessage.setText(msg);
        statusMessage.setStyle(isError ? "-fx-text-fill: #ef4444;" : "-fx-text-fill: #15803d;");
        statusMessage.setVisible(true);
    }

    // Navigation callbacks
    @FXML private void goDashboard() { SceneNavigator.showDashboard(); }
    @FXML private void goRestaurants() { SceneNavigator.showRestaurants(); }
    @FXML private void goMenus() { SceneNavigator.showMenus(); }
    @FXML private void goOrders() { SceneNavigator.showOrders(); }
    @FXML private void goBatches() { SceneNavigator.showBatches(); }
    @FXML private void goSurplus() { SceneNavigator.showSurplus(); }
    @FXML private void goGroupOrders() { SceneNavigator.showGroupOrders(); }
    @FXML private void handleLogout() {
        SceneNavigator.setCurrentUser(null);
        SceneNavigator.showLogin();
    }
}
