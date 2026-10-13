package com.app.desktop;

import com.app.dao.UserDAO;
import com.app.dao_implementation.UserDAOImpl;
import com.app.models.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.mindrot.jbcrypt.BCrypt;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final UserDAO userDAO = new UserDAOImpl();

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            showError("Username and password are required.");
            return;
        }

        try {
            User user = userDAO.getUserByUsername(username.trim());
            if (user != null && BCrypt.checkpw(password, user.getPassword())) {
                SceneNavigator.setCurrentUser(user);
                SceneNavigator.showDashboard();
            } else {
                showError("Invalid username or password.");
            }
        } catch (Exception e) {
            showError("Database connection failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleQuickAdmin() {
        User admin = new User();
        admin.setUserId(1);
        admin.setUsername("admin");
        admin.setName("Admin User");
        admin.setRole("admin");
        SceneNavigator.setCurrentUser(admin);
        SceneNavigator.showDashboard();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
