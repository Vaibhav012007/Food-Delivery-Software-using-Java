package com.app.desktop;

import com.app.dao.RestaurantDAO;
import com.app.dao_implementation.RestaurantDAOImpl;
import com.app.models.Restaurant;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

public class RestaurantManagementController {

    @FXML private TableView<Restaurant> restaurantTable;
    @FXML private TableColumn<Restaurant, Number> idCol;
    @FXML private TableColumn<Restaurant, String> nameCol;
    @FXML private TableColumn<Restaurant, String> cuisineCol;
    @FXML private TableColumn<Restaurant, String> addressCol;
    @FXML private TableColumn<Restaurant, Number> ratingCol;
    @FXML private TableColumn<Restaurant, Number> timeCol;

    @FXML private TextField nameInput;
    @FXML private TextField cuisineInput;
    @FXML private TextField addressInput;
    @FXML private TextField ratingInput;
    @FXML private TextField deliveryTimeInput;
    @FXML private Label statusMessage;

    private final RestaurantDAO restaurantDAO = new RestaurantDAOImpl();

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getRestaurantId()));
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        cuisineCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCuisineType()));
        addressCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAddress()));
        ratingCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getRating()));
        timeCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getDeliveryTime()));

        loadRestaurants();
    }

    @FXML
    public void loadRestaurants() {
        try {
            List<Restaurant> list = restaurantDAO.getAllRestaurants();
            ObservableList<Restaurant> obsList = FXCollections.observableArrayList(list);
            restaurantTable.setItems(obsList);
        } catch (Exception e) {
            showMsg("Failed to load restaurants: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleAddRestaurant() {
        String name = nameInput.getText();
        String cuisine = cuisineInput.getText();
        String address = addressInput.getText();
        String ratingStr = ratingInput.getText();
        String timeStr = deliveryTimeInput.getText();

        if (name == null || name.trim().isEmpty()) {
            showMsg("Restaurant name is required.", true);
            return;
        }

        try {
            Restaurant r = new Restaurant();
            r.setName(name.trim());
            r.setCuisineType(cuisine != null ? cuisine.trim() : "General");
            r.setAddress(address != null ? address.trim() : "Campus");
            r.setCity("Campus");
            r.setRating(ratingStr != null && !ratingStr.isEmpty() ? Double.parseDouble(ratingStr) : 4.0);
            r.setDeliveryTime(timeStr != null && !timeStr.isEmpty() ? Integer.parseInt(timeStr) : 30);
            r.setActive(true);

            restaurantDAO.addRestaurant(r);
            showMsg("Restaurant added successfully!", false);

            nameInput.clear();
            cuisineInput.clear();
            addressInput.clear();
            loadRestaurants();
        } catch (Exception e) {
            showMsg("Failed to add restaurant: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleDelete() {
        Restaurant selected = restaurantTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMsg("Please select a restaurant to delete.", true);
            return;
        }

        try {
            restaurantDAO.deleteRestaurant(selected.getRestaurantId());
            showMsg("Deleted " + selected.getName(), false);
            loadRestaurants();
        } catch (Exception e) {
            showMsg("Failed to delete: " + e.getMessage(), true);
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
