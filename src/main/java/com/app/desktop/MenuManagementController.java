package com.app.desktop;

import com.app.dao.MenuItemDAO;
import com.app.dao.RestaurantDAO;
import com.app.dao_implementation.MenuItemDAOImpl;
import com.app.dao_implementation.RestaurantDAOImpl;
import com.app.models.MenuItem;
import com.app.models.Restaurant;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.util.List;

public class MenuManagementController {

    @FXML private TableView<MenuItem> menuTable;
    @FXML private TableColumn<MenuItem, Number> idCol;
    @FXML private TableColumn<MenuItem, String> nameCol;
    @FXML private TableColumn<MenuItem, String> categoryCol;
    @FXML private TableColumn<MenuItem, Number> priceCol;
    @FXML private TableColumn<MenuItem, String> vegCol;
    @FXML private TableColumn<MenuItem, String> availableCol;
    @FXML private TableColumn<MenuItem, Number> stockCol;

    @FXML private ComboBox<Restaurant> restaurantFilter;
    @FXML private TextField itemNameInput;
    @FXML private TextField categoryInput;
    @FXML private TextField priceInput;
    @FXML private TextField stockInput;
    @FXML private CheckBox isVegCheckbox;
    @FXML private CheckBox isAvailableCheckbox;
    @FXML private Label statusMessage;

    private final MenuItemDAO menuItemDAO = new MenuItemDAOImpl();
    private final RestaurantDAO restaurantDAO = new RestaurantDAOImpl();

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getMenuItemId()));
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        categoryCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        priceCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getPrice()));
        vegCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isVeg() ? "Veg" : "Non-Veg"));
        availableCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isAvailable() ? "Yes" : "No"));
        stockCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getStockQuantity()));

        setupRestaurantFilter();
    }

    private void setupRestaurantFilter() {
        try {
            List<Restaurant> list = restaurantDAO.getAllRestaurants();
            restaurantFilter.setItems(FXCollections.observableArrayList(list));
            restaurantFilter.setConverter(new StringConverter<Restaurant>() {
                @Override
                public String toString(Restaurant r) {
                    return r != null ? r.getName() : "";
                }
                @Override
                public Restaurant fromString(String string) {
                    return null;
                }
            });

            if (!list.isEmpty()) {
                restaurantFilter.getSelectionModel().select(0);
                loadMenuItems();
            }
        } catch (Exception e) {
            showMsg("Failed to load restaurants: " + e.getMessage(), true);
        }
    }

    @FXML
    private void filterChanged() {
        loadMenuItems();
    }

    @FXML
    public void loadMenuItems() {
        Restaurant selected = restaurantFilter.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            List<MenuItem> items = menuItemDAO.getMenuItemsByRestaurantId(selected.getRestaurantId());
            ObservableList<MenuItem> obs = FXCollections.observableArrayList(items);
            menuTable.setItems(obs);
        } catch (Exception e) {
            showMsg("Failed to load items: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleAddItem() {
        Restaurant selected = restaurantFilter.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMsg("Please select a restaurant first.", true);
            return;
        }

        String name = itemNameInput.getText();
        String cat = categoryInput.getText();
        String priceStr = priceInput.getText();
        String stockStr = stockInput.getText();

        if (name == null || name.trim().isEmpty() || priceStr == null || priceStr.trim().isEmpty()) {
            showMsg("Name and price are required.", true);
            return;
        }

        try {
            MenuItem item = new MenuItem();
            item.setRestaurantId(selected.getRestaurantId());
            item.setName(name.trim());
            item.setCategory(cat != null ? cat.trim() : "General");
            item.setPrice(Double.parseDouble(priceStr.trim()));
            item.setVeg(isVegCheckbox.isSelected());
            item.setAvailable(isAvailableCheckbox.isSelected());
            item.setStockQuantity(stockStr != null && !stockStr.isEmpty() ? Integer.parseInt(stockStr.trim()) : 50);

            menuItemDAO.addMenuItem(item);
            showMsg("Item added successfully!", false);

            itemNameInput.clear();
            categoryInput.clear();
            priceInput.clear();
            loadMenuItems();
        } catch (Exception e) {
            showMsg("Failed to add item: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleToggleAvailability() {
        MenuItem selected = menuTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMsg("Select an item to toggle availability.", true);
            return;
        }

        try {
            boolean newStatus = !selected.isAvailable();
            menuItemDAO.updateAvailability(selected.getMenuItemId(), newStatus);
            showMsg("Updated " + selected.getName() + " to " + (newStatus ? "Available" : "Unavailable"), false);
            loadMenuItems();
        } catch (Exception e) {
            showMsg("Failed to update status: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleDelete() {
        MenuItem selected = menuTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMsg("Select an item to delete.", true);
            return;
        }

        try {
            menuItemDAO.deleteMenuItem(selected.getMenuItemId());
            showMsg("Deleted " + selected.getName(), false);
            loadMenuItems();
        } catch (Exception e) {
            showMsg("Failed to delete item: " + e.getMessage(), true);
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
