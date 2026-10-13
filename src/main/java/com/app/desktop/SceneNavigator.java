package com.app.desktop;

import com.app.models.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Navigation manager for the JavaFX Desktop Application.
 * Handles switching between the 8 application scenes and tracking user session.
 */
public class SceneNavigator {

    private static Stage primaryStage;
    private static User currentUser;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void loadScene(String fxmlPath, String title) {
        try {
            URL url = SceneNavigator.class.getResource("/fxml/" + fxmlPath);
            if (url == null) {
                System.err.println("[SceneNavigator] Could not find FXML resource: /fxml/" + fxmlPath);
                return;
            }
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            Scene scene = new Scene(root, 1024, 700);
            primaryStage.setTitle("FoodXpress Desktop - " + title);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("[SceneNavigator] Failed to load FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }

    // Screen Navigations
    public static void showLogin() {
        loadScene("login.fxml", "Login");
    }

    public static void showDashboard() {
        loadScene("admin_dashboard.fxml", "Dashboard");
    }

    public static void showRestaurants() {
        loadScene("restaurant_management.fxml", "Restaurants");
    }

    public static void showMenus() {
        loadScene("menu_management.fxml", "Menu Management");
    }

    public static void showOrders() {
        loadScene("order_management.fxml", "Order Management");
    }

    public static void showBatches() {
        loadScene("delivery_batch.fxml", "Delivery Batching");
    }

    public static void showSurplus() {
        loadScene("surplus_marketplace.fxml", "Surplus Marketplace");
    }

    public static void showGroupOrders() {
        loadScene("group_order.fxml", "Group Orders");
    }
}
