package com.app.desktop;

import com.app.concurrency.AppExecutors;
import com.app.concurrency.NotificationService;
import com.app.utility.DBConnection;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main Entry Point for the FoodXpress JavaFX Desktop Application.
 * 
 * Features:
 * - Bootstraps the HikariCP database pool and background workers.
 * - Initializes the navigation coordinator.
 * - Handles graceful application shutdown on stage close.
 */
public class FoodXpressDesktopApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        System.out.println("[FoodXpressDesktopApp] Initializing application...");

        // Initialize shared background infrastructure
        try {
            DBConnection.init();
        } catch (Exception e) {
            System.err.println("[FoodXpressDesktopApp] DB pool init notice: " + e.getMessage());
        }

        AppExecutors.init();
        NotificationService.getInstance().start();

        SceneNavigator.setPrimaryStage(primaryStage);
        SceneNavigator.showLogin();

        primaryStage.setOnCloseRequest(event -> {
            System.out.println("[FoodXpressDesktopApp] Closing desktop application...");
            NotificationService.getInstance().shutdown();
            AppExecutors.shutdown();
            DBConnection.closePool();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
