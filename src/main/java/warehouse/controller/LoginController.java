package warehouse.controller;

import warehouse.model.Role;
import warehouse.model.User;
import warehouse.service.AuthService;
import warehouse.util.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

public class LoginController {

    private static final String VIEW_PATH = "/warehouse/view/";

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void handleLogin() {
        hideError();

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter your username and password.");
            return;
        }

        Optional<User> user;
        try {
            user = authService.login(username, password);
        } catch (Exception e) {
            showError("Could not connect to the database.");
            e.printStackTrace();
            return;
        }

        if (user.isEmpty()) {
            showError("Invalid username or password.");
            passwordField.clear();
            return;
        }

        UserSession.login(user.get());
        openDashboard(user.get().getRole());
    }

    private void openDashboard(Role role) {
        String fxmlFile = switch (role) {
            case ADMIN -> "admin.fxml";
            case OWNER -> "owner.fxml";
            case AGENT -> "agent.fxml";
        };

        URL location = getClass().getResource(VIEW_PATH + fxmlFile);
        if (location == null) {
            showError("The " + role + " screen is not ready yet (" + fxmlFile + ").");
            UserSession.logout();
            return;
        }

        try {
            Parent root = FXMLLoader.load(location);
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Warehouse Management System - " + role);
            stage.centerOnScreen();
        } catch (IOException e) {
            showError("Could not open the " + role + " screen.");
            UserSession.logout();
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
