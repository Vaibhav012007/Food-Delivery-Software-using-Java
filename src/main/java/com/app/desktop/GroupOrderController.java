package com.app.desktop;

import com.app.dao.MenuItemDAO;
import com.app.dao_implementation.MenuItemDAOImpl;
import com.app.exceptions.AppException;
import com.app.models.GroupCartItem;
import com.app.models.GroupOrder;
import com.app.models.MenuItem;
import com.app.service.GroupOrderService;
import com.app.service.GroupOrderServiceImpl;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class GroupOrderController {

    @FXML private TableView<GroupOrder> groupTable;
    @FXML private TableColumn<GroupOrder, Number> idCol;
    @FXML private TableColumn<GroupOrder, Number> creatorCol;
    @FXML private TableColumn<GroupOrder, Number> restaurantCol;
    @FXML private TableColumn<GroupOrder, String> inviteCodeCol;
    @FXML private TableColumn<GroupOrder, String> deadlineCol;
    @FXML private TableColumn<GroupOrder, String> statusCol;

    @FXML private Label selectedGroupLabel;
    @FXML private ListView<String> cartItemsList;
    @FXML private ListView<String> memberTotalsList;
    @FXML private Button checkoutButton;
    @FXML private TextField hostUserIdInput;
    @FXML private TextField restaurantIdInput;
    @FXML private Label statusMessage;

    private final GroupOrderService groupOrderService = new GroupOrderServiceImpl();
    private final MenuItemDAO menuItemDAO = new MenuItemDAOImpl();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, hh:mm a");

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getGroupOrderId()));
        creatorCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getCreatorId()));
        restaurantCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getRestaurantId()));
        inviteCodeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getInviteCode()));
        deadlineCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDeadline() != null ? dateFormat.format(data.getValue().getDeadline()) : "-"));
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));

        groupTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showGroupDetails(newVal);
            }
        });

        loadGroups();
    }

    @FXML
    public void loadGroups() {
        try {
            List<GroupOrder> groups = groupOrderService.getOpenGroups();
            groupTable.setItems(FXCollections.observableArrayList(groups));
        } catch (Exception e) {
            showMsg("Failed to load group orders: " + e.getMessage(), true);
        }
    }

    private void showGroupDetails(GroupOrder group) {
        selectedGroupLabel.setText("Group #" + group.getGroupOrderId() + " (" + group.getInviteCode() + ")");
        checkoutButton.setDisable(!"open".equalsIgnoreCase(group.getStatus()));

        // Load cart items
        try {
            List<GroupCartItem> items = groupOrderService.getGroupCart(group.getGroupOrderId());
            Set<Integer> menuIds = new HashSet<>();
            for (GroupCartItem i : items) menuIds.add(i.getMenuItemId());
            Map<Integer, MenuItem> menuMap = menuItemDAO.getMenuItemsAsMap(menuIds);

            List<String> listItems = new ArrayList<>();
            for (GroupCartItem item : items) {
                MenuItem mi = menuMap.get(item.getMenuItemId());
                String name = mi != null ? mi.getName() : ("Item #" + item.getMenuItemId());
                listItems.add("User #" + item.getUserId() + ": " + name + " × " + item.getQuantity());
            }
            cartItemsList.setItems(FXCollections.observableArrayList(listItems));
        } catch (Exception e) {
            cartItemsList.setItems(FXCollections.observableArrayList("Error loading cart"));
        }

        // Load member subtotals
        try {
            Map<Integer, Double> totals = groupOrderService.getMemberTotals(group.getGroupOrderId());
            List<String> totalItems = new ArrayList<>();
            for (Map.Entry<Integer, Double> entry : totals.entrySet()) {
                totalItems.add("User #" + entry.getKey() + ": ₹" + String.format("%.2f", entry.getValue()));
            }
            memberTotalsList.setItems(FXCollections.observableArrayList(totalItems));
        } catch (Exception e) {
            memberTotalsList.setItems(FXCollections.observableArrayList("Error calculating totals"));
        }
    }

    @FXML
    private void handleCreateGroup() {
        try {
            int hostId = Integer.parseInt(hostUserIdInput.getText().trim());
            int restId = Integer.parseInt(restaurantIdInput.getText().trim());

            int groupId = groupOrderService.createGroup(hostId, restId, 60);
            showMsg("Group order #" + groupId + " created successfully!", false);
            loadGroups();
        } catch (Exception e) {
            showMsg("Failed to create group order: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleCheckout() {
        GroupOrder selected = groupTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            int orderId = groupOrderService.checkoutGroup(selected.getGroupOrderId(), selected.getCreatorId(), "cash_on_delivery", null);
            showMsg("Checkout completed! Resulting Order ID: #" + orderId, false);
            loadGroups();
        } catch (AppException e) {
            showMsg("Checkout failed: " + e.getMessage(), true);
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
