package com.app.desktop;

import com.app.dao.DeliveryBatchDAO;
import com.app.dao.OrderDAO;
import com.app.dao.RestaurantDAO;
import com.app.dao.SurplusListingDAO;
import com.app.dao_implementation.DeliveryBatchDAOImpl;
import com.app.dao_implementation.OrderDAOImpl;
import com.app.dao_implementation.RestaurantDAOImpl;
import com.app.dao_implementation.SurplusListingDAOImpl;
import com.app.models.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class AdminDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label restaurantCountLabel;
    @FXML private Label orderCountLabel;
    @FXML private Label batchCountLabel;
    @FXML private Label surplusCountLabel;

    private final RestaurantDAO restaurantDAO = new RestaurantDAOImpl();
    private final OrderDAO orderDAO = new OrderDAOImpl();
    private final DeliveryBatchDAO batchDAO = new DeliveryBatchDAOImpl();
    private final SurplusListingDAO surplusDAO = new SurplusListingDAOImpl();

    @FXML
    public void initialize() {
        User user = SceneNavigator.getCurrentUser();
        if (user != null) {
            welcomeLabel.setText("Logged in as " + user.getName() + " (" + user.getRole() + ")");
        }
        loadMetrics();
    }

    private void loadMetrics() {
        try {
            int restaurants = restaurantDAO.getAllRestaurants().size();
            restaurantCountLabel.setText(String.valueOf(restaurants));
        } catch (Exception e) {
            restaurantCountLabel.setText("0");
        }

        try {
            int orders = orderDAO.getOrdersByStatus("placed").size() +
                         orderDAO.getOrdersByStatus("accepted").size();
            orderCountLabel.setText(String.valueOf(orders));
        } catch (Exception e) {
            orderCountLabel.setText("0");
        }

        try {
            int batches = batchDAO.getAllBatches(100).size();
            batchCountLabel.setText(String.valueOf(batches));
        } catch (Exception e) {
            batchCountLabel.setText("0");
        }

        try {
            int surplus = surplusDAO.getActiveListings().size();
            surplusCountLabel.setText(String.valueOf(surplus));
        } catch (Exception e) {
            surplusCountLabel.setText("0");
        }
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
