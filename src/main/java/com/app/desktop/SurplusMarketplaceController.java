package com.app.desktop;

import com.app.models.SurplusListing;
import com.app.service.SurplusService;
import com.app.service.SurplusServiceImpl;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;

public class SurplusMarketplaceController {

    @FXML private TableView<SurplusListing> surplusTable;
    @FXML private TableColumn<SurplusListing, Number> idCol;
    @FXML private TableColumn<SurplusListing, Number> restaurantCol;
    @FXML private TableColumn<SurplusListing, Number> itemCol;
    @FXML private TableColumn<SurplusListing, Number> origPriceCol;
    @FXML private TableColumn<SurplusListing, Number> discPriceCol;
    @FXML private TableColumn<SurplusListing, Number> discountCol;
    @FXML private TableColumn<SurplusListing, Number> qtyCol;
    @FXML private TableColumn<SurplusListing, String> expiryCol;
    @FXML private TableColumn<SurplusListing, String> statusCol;

    @FXML private TextField restaurantIdInput;
    @FXML private TextField menuItemIdInput;
    @FXML private TextField quantityInput;
    @FXML private TextField origPriceInput;
    @FXML private TextField discPriceInput;
    @FXML private TextField expiryMinutesInput;
    @FXML private Label statusMessage;

    private final SurplusService surplusService = new SurplusServiceImpl();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, hh:mm a");

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getListingId()));
        restaurantCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getRestaurantId()));
        itemCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getMenuItemId()));
        origPriceCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getOriginalPrice()));
        discPriceCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getDiscountedPrice()));
        discountCol.setCellValueFactory(data -> new SimpleDoubleProperty(Math.round(data.getValue().getDiscountPercentage())));
        qtyCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getAvailableQuantity()));
        expiryCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getExpiryTime() != null ? dateFormat.format(data.getValue().getExpiryTime()) : "-"));
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));

        loadListings();
    }

    @FXML
    public void loadListings() {
        try {
            List<SurplusListing> listings = surplusService.getActiveListings();
            surplusTable.setItems(FXCollections.observableArrayList(listings));
        } catch (Exception e) {
            showMsg("Failed to load listings: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleCreateListing() {
        try {
            int restId = Integer.parseInt(restaurantIdInput.getText().trim());
            int itemId = Integer.parseInt(menuItemIdInput.getText().trim());
            int qty = Integer.parseInt(quantityInput.getText().trim());
            double origPrice = Double.parseDouble(origPriceInput.getText().trim());
            double discPrice = Double.parseDouble(discPriceInput.getText().trim());
            int expMin = Integer.parseInt(expiryMinutesInput.getText().trim());

            Timestamp expiryTime = new Timestamp(System.currentTimeMillis() + (long) expMin * 60 * 1000);
            int id = surplusService.createListing(restId, itemId, qty, origPrice, discPrice, expiryTime);

            showMsg("Listing #" + id + " created successfully!", false);
            loadListings();
        } catch (Exception e) {
            showMsg("Failed to create listing: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleExpireListings() {
        try {
            int count = surplusService.expireListings();
            showMsg("Expiry check completed. Expired: " + count + " listings.", false);
            loadListings();
        } catch (Exception e) {
            showMsg("Expiry sweep failed: " + e.getMessage(), true);
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
