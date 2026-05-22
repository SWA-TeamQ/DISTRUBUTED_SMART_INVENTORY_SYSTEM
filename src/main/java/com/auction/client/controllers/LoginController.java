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
        
        com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
        com.auction.shared.interfaces.IAuctionService service = context.getRmiProvider().getService();
        String token;
        try {
            token = service.login(username, password);
            context.setSessionToken(token);
            context.setUsername(username);
            String role = service.getMyRole(token);
            context.setUserRole(role);
        } catch (com.auction.shared.exceptions.AuctionException ae) {
            if (statusLabel != null) statusLabel.setText("Authentication failed: " + ae.getMessage());
            return;
        } catch (java.rmi.RemoteException re) {
            if (statusLabel != null) statusLabel.setText("Connection error: " + re.getMessage());
            re.printStackTrace();
            return;
        } catch (Exception e) {
            if (statusLabel != null) statusLabel.setText("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Load UI in a separate try so view-loading errors don't look like authentication failures
        try {
            if (com.auction.shared.Constants.ADMIN.equals(context.getUserRole())) {
                context.getViewLoader().loadView("admin_panel.fxml");
            } else {
                context.getViewLoader().loadView("seller_dashboard.fxml");
            }
        } catch (IOException ioe) {
            if (statusLabel != null) statusLabel.setText("Login succeeded but failed to load UI: " + ioe.getMessage());
            ioe.printStackTrace();
        } catch (Exception e) {
            if (statusLabel != null) statusLabel.setText("Failed to open dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
