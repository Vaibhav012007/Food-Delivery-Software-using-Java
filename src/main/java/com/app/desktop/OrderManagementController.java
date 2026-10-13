package com.app.desktop;

import com.app.dao.MenuItemDAO;
import com.app.dao.OrderDAO;
import com.app.dao_implementation.MenuItemDAOImpl;
import com.app.dao_implementation.OrderDAOImpl;
import com.app.exceptions.AppException;
import com.app.models.MenuItem;
import com.app.models.Order;
import com.app.models.OrderItem;
import com.app.models.OrderStatus;
import com.app.service.OrderService;
import com.app.service.OrderServiceImpl;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class OrderManagementController {

    @FXML private TableView<Order> orderTable;
    @FXML private TableColumn<Order, Number> idCol;
    @FXML private TableColumn<Order, Number> userCol;
    @FXML private TableColumn<Order, Number> restaurantCol;
    @FXML private TableColumn<Order, Number> amountCol;
    @FXML private TableColumn<Order, String> statusCol;
    @FXML private TableColumn<Order, String> dateCol;

    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private Label selectedOrderLabel;
    @FXML private Label currentStateLabel;
    @FXML private ComboBox<String> nextStatusCombo;
    @FXML private Label statusMessage;
    @FXML private ListView<String> orderItemsList;

    private final OrderDAO orderDAO = new OrderDAOImpl();
    private final OrderService orderService = new OrderServiceImpl();
    private final MenuItemDAO menuItemDAO = new MenuItemDAOImpl();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, hh:mm a");

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getOrderId()));
        userCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getUserId()));
        restaurantCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getRestaurantId()));
        amountCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getTotalAmount()));
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCreatedAt() != null ? dateFormat.format(data.getValue().getCreatedAt()) : "-"));

        statusFilterCombo.setItems(FXCollections.observableArrayList(
                "All Active", "placed", "accepted", "preparing", "ready_for_pickup", "out_for_delivery", "delivered", "cancelled"
        ));
        statusFilterCombo.getSelectionModel().select(0);

        orderTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showOrderDetails(newVal);
            }
        });

        loadOrders();
    }

    @FXML
    public void loadOrders() {
        try {
            String filter = statusFilterCombo.getSelectionModel().getSelectedItem();
            List<Order> orders;

            if (filter == null || "All Active".equals(filter)) {
                orders = new ArrayList<>();
                orders.addAll(orderDAO.getOrdersByStatus("placed"));
                orders.addAll(orderDAO.getOrdersByStatus("accepted"));
                orders.addAll(orderDAO.getOrdersByStatus("preparing"));
                orders.addAll(orderDAO.getOrdersByStatus("ready_for_pickup"));
                orders.addAll(orderDAO.getOrdersByStatus("out_for_delivery"));
            } else {
                orders = orderDAO.getOrdersByStatus(filter);
            }

            orderTable.setItems(FXCollections.observableArrayList(orders));
        } catch (Exception e) {
            showMsg("Failed to load orders: " + e.getMessage(), true);
        }
    }

    @FXML
    private void filterChanged() {
        loadOrders();
    }

    private void showOrderDetails(Order order) {
        selectedOrderLabel.setText("Order #" + order.getOrderId() + " (User #" + order.getUserId() + ")");
        currentStateLabel.setText(order.getStatus().toUpperCase());

        // Determine valid next transitions using OrderStatus state machine
        try {
            OrderStatus current = OrderStatus.fromDbValue(order.getStatus());
            List<String> validNext = new ArrayList<>();
            for (OrderStatus candidate : OrderStatus.values()) {
                if (current.canTransitionTo(candidate)) {
                    validNext.add(candidate.getDbValue());
                }
            }
            nextStatusCombo.setItems(FXCollections.observableArrayList(validNext));
            if (!validNext.isEmpty()) {
                nextStatusCombo.getSelectionModel().select(0);
            }
        } catch (Exception e) {
            nextStatusCombo.setItems(FXCollections.emptyObservableList());
        }

        // Load items
        try {
            List<OrderItem> items = orderDAO.getOrderItems(order.getOrderId());
            Set<Integer> menuIds = new HashSet<>();
            for (OrderItem i : items) menuIds.add(i.getMenuItemId());
            Map<Integer, MenuItem> menuMap = menuItemDAO.getMenuItemsAsMap(menuIds);

            List<String> listItems = new ArrayList<>();
            for (OrderItem item : items) {
                MenuItem mi = menuMap.get(item.getMenuItemId());
                String name = mi != null ? mi.getName() : ("Item #" + item.getMenuItemId());
                listItems.add(name + " × " + item.getQuantity() + " (₹" + (item.getPriceAtOrder() * item.getQuantity()) + ")");
            }
            orderItemsList.setItems(FXCollections.observableArrayList(listItems));
        } catch (Exception e) {
            orderItemsList.setItems(FXCollections.observableArrayList("Error loading items"));
        }
    }

    @FXML
    private void handleStatusTransition() {
        Order selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMsg("Please select an order first.", true);
            return;
        }

        String nextStatus = nextStatusCombo.getSelectionModel().getSelectedItem();
        if (nextStatus == null) {
            showMsg("No valid transition selected.", true);
            return;
        }

        try {
            orderService.updateStatus(selected.getOrderId(), nextStatus, 1, "admin");
            showMsg("Transitioned to " + nextStatus + " successfully!", false);
            loadOrders();
        } catch (AppException e) {
            showMsg("State machine violation: " + e.getMessage(), true);
        } catch (Exception e) {
            showMsg("Error: " + e.getMessage(), true);
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
