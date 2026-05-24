package com.auction.client.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    @FXML private javafx.scene.control.Label statusLabel;
    @FXML private javafx.scene.control.TitledPane adminErrorPanel;
    @FXML private javafx.scene.control.TextArea adminErrorDetails;

    @FXML
    public void initialize() {
        if (!com.auction.client.core.ClientContext.getInstance().getRmiProvider().isConnected()) {
            System.err.println("Not connected to RMI server.");
        }
        // Hide status label when empty and show only on errors/messages
        if (statusLabel != null) {
            statusLabel.setVisible(false);
            statusLabel.managedProperty().bind(statusLabel.visibleProperty());
            statusLabel.textProperty().addListener((obs, oldText, newText) -> {
                statusLabel.setVisible(newText != null && !newText.trim().isEmpty());
            });
        }

        if (adminErrorPanel != null) {
            adminErrorPanel.setVisible(false);
            adminErrorPanel.setManaged(false);
            adminErrorPanel.setExpanded(false);
        }
    }

    @FXML
    private void handleNavigateToRegister(javafx.scene.input.MouseEvent event) {
        try {
            com.auction.client.core.ClientContext.getInstance().getViewLoader().loadView("registration.fxml");
        } catch (IOException e) {
            if (statusLabel != null) {
                statusLabel.setText("Unable to open registration page: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (adminErrorPanel != null) {
            adminErrorPanel.setVisible(false);
            adminErrorPanel.setManaged(false);
            adminErrorPanel.setExpanded(false);
        }

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
                context.getViewLoader().loadView("user_dashboard.fxml");
            }
        } catch (IOException ioe) {
            if (statusLabel != null) statusLabel.setText("Login succeeded but failed to load UI: " + ioe.getMessage());
            ioe.printStackTrace();
        } catch (Exception e) {
            if (statusLabel != null) statusLabel.setText("Login failed: " + e.getMessage());

            boolean isAdminAttempt = com.auction.shared.Constants.DEFAULT_ADMIN_USERNAME.equalsIgnoreCase(username);
            if (isAdminAttempt && adminErrorPanel != null && adminErrorDetails != null) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                adminErrorDetails.setText(sw.toString());
                adminErrorPanel.setVisible(true);
                adminErrorPanel.setManaged(true);
            }

            e.printStackTrace();
        }
    }
}
