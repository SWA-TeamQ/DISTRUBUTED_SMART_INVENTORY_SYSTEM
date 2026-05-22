package com.auction.client.controllers;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    @FXML private javafx.scene.control.Label statusLabel;

    @FXML
    public void initialize() {
        if (!com.auction.client.core.ClientContext.getInstance().getRmiProvider().isConnected()) {
            System.err.println("Not connected to RMI server.");
        }
    }

    @FXML
    private void handleNavigateToRegister() throws IOException {
        com.auction.client.core.ClientContext.getInstance().getViewLoader().loadView("registration.fxml");
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            if (statusLabel != null) statusLabel.setText("Please enter credentials.");
            return;
        }
        
        try {
            com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
            com.auction.shared.interfaces.IAuctionService service = context.getRmiProvider().getService();
            String token = service.login(username, password);
            context.setSessionToken(token);
            context.setUsername(username);
            String role = service.getSessionRole(token);
            context.setUserRole(role);

            if (com.auction.shared.Constants.ADMIN.equals(role)) {
                context.getViewLoader().loadView("admin_panel.fxml");
            } else {
                context.getViewLoader().loadView("user_dashboard.fxml");
            }
            if (statusLabel != null) statusLabel.setText("Login successful.");
        } catch (com.auction.shared.exceptions.AuctionException e) {
            if (statusLabel != null) statusLabel.setText(e.getMessage());
        } catch (java.io.IOException e) {
            if (statusLabel != null) statusLabel.setText("Login succeeded, but the dashboard could not open: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            if (statusLabel != null) statusLabel.setText("Login failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
