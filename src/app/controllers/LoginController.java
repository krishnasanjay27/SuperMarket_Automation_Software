package app.controllers;

import app.AlertHelper;
import app.SceneNavigator;
import app.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import model.UserAccount;
import service.AuthService;

public class LoginController {

    @FXML private TextField     userIdField;
    @FXML private PasswordField passwordField;

    private final AuthService authService = new AuthService();

    @FXML
    private void handleLogin() {
        String userId   = userIdField.getText().trim();
        String password = passwordField.getText().trim();

        if (userId.isEmpty() || password.isEmpty()) {
            AlertHelper.showError("Validation Error", "User ID and Password must not be empty.");
            return;
        }

        UserAccount user = authService.login(userId, password);

        if (user == null) {
            AlertHelper.showError("Login Failed", "Invalid credentials or inactive account.");
            return;
        }

        SessionManager session = SessionManager.getInstance();
        session.setUserId(user.getUserId());
        session.setRole(user.getRole());

        switch (user.getRole()) {
            case "SalesStaff":
                SceneNavigator.navigateTo("sales_dashboard.fxml", "Sales Panel");
                break;
            case "InventoryStaff":
                SceneNavigator.navigateTo("inventory_dashboard.fxml", "Inventory Panel");
                break;
            case "Manager":
                SceneNavigator.navigateTo("manager_dashboard.fxml", "Manager Panel");
                break;
            default:
                AlertHelper.showError("Unknown Role", "Role '" + user.getRole() + "' is not recognized.");
        }
    }

    @FXML
    private void handleExit() {
        System.exit(0);
    }
}
