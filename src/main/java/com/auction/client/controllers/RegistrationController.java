package com.auction.client.controllers;

import com.auction.client.core.ClientContext;
import com.auction.shared.Constants;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RegistrationController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    @FXML
    private void handleNavigateToLogin(javafx.scene.input.MouseEvent event) {
        try {
            ClientContext.getInstance().getViewLoader().loadView("login.fxml");
        } catch (IOException e) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Unable to open login page: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String role = Constants.USER;

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please fill in all fields.");
            return;
        }

        try {
            ClientContext context = ClientContext.getInstance();
            com.auction.shared.interfaces.IAuctionService service = context.getRmiProvider().getService();
            service.register(username, password, role);
            context.setSessionToken(null);
            context.setUserRole(null);
            context.setUsername(null);
            context.getViewLoader().loadView("login.fxml");
        } catch (Exception e) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Registration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
