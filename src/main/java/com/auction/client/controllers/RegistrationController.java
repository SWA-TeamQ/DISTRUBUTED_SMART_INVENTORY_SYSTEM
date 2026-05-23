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
    public void initialize() {
        if (statusLabel != null) {
            statusLabel.setVisible(false);
            statusLabel.managedProperty().bind(statusLabel.visibleProperty());
            statusLabel.textProperty().addListener((obs, oldText, newText) ->
                statusLabel.setVisible(newText != null && !newText.trim().isEmpty())
            );
        }
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String role = Constants.USER;

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: #f85149;");
            statusLabel.setText("Please fill in all fields.");
            return;
        }

        try {
            ClientContext context = ClientContext.getInstance();
            com.auction.shared.interfaces.IAuctionService service = context.getRmiProvider().getService();
            service.register(username, password, role);
            String token = service.login(username, password);
            context.setSessionToken(token);
            context.setUsername(username);
            context.setUserRole(service.getMyRole(token));
            statusLabel.setStyle("-fx-text-fill: #3fb950;");
            statusLabel.setText("Registration successful! Entering the application...");
            context.getViewLoader().loadView("user_dashboard.fxml");
        } catch (Exception e) {
            statusLabel.setStyle("-fx-text-fill: #f85149;");
            statusLabel.setText("Registration failed. Please try again.");
            e.printStackTrace();
        }
    }
}
