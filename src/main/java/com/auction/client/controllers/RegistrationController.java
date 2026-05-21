package com.auction.client.controllers;

import com.auction.client.core.ClientContext;
import com.auction.shared.Constants;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RegistrationController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

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
            context.getRmiProvider().getService().register(username, password, role);
            statusLabel.setStyle("-fx-text-fill: green;");
            statusLabel.setText("Registration successful!");
            // Optionally navigate back to login
            context.getViewLoader().loadView("login.fxml");
        } catch (Exception e) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Registration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
