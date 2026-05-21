package com.auction.client.controllers;

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
    private void handleNavigateToRegister() {
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
            
            // For now, since user role isn't explicitly returned from login but only the token,
            // we will fetch all users if ADMIN or just navigate based on some hardcoded logic.
            // Wait, does the RMI login return role or just token? Interface says: String login(...)
            // We should just route to Admin if username == default admin, else Seller/Bidder.
            if (com.auction.shared.Constants.DEFAULT_ADMIN_USERNAME.equals(username)) {
                context.setUserRole(com.auction.shared.Constants.ADMIN);
                context.getViewLoader().loadView("admin_panel.fxml");
            } else {
                // Defaulting to seller dashboard for M1 tasks
                context.setUserRole(com.auction.shared.Constants.SELLER);
                context.getViewLoader().loadView("seller_dashboard.fxml");
            }
        } catch (Exception e) {
            if (statusLabel != null) statusLabel.setText("Login failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
