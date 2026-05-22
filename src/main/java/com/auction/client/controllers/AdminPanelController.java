package com.auction.client.controllers;

import javafx.fxml.FXML;

public class AdminPanelController {

    @FXML private javafx.scene.control.TableView<com.auction.shared.models.User> usersTable;
    @FXML private javafx.scene.control.ListView<String> auditListView;
    @FXML private javafx.scene.control.TextField usernameField;
    @FXML private javafx.scene.control.PasswordField passwordField;
    @FXML private javafx.scene.control.ComboBox<String> roleCombo;
    @FXML private javafx.scene.control.Label statusLabel;

    @FXML
    public void initialize() {
        roleCombo.getItems().addAll(
            com.auction.shared.Constants.USER, 
            com.auction.shared.Constants.ADMIN
        );
        roleCombo.getSelectionModel().selectFirst();
        
        loadUsers();
        loadAuditLogs();
    }

    private void loadUsers() {
        try {
            com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
            java.util.List<com.auction.shared.models.User> users = context.getRmiProvider().getService().getAllUsers(context.getSessionToken());
            usersTable.getItems().setAll(users);
        } catch (Exception e) {
            statusLabel.setText("Failed to load users: " + e.getMessage());
        }
    }

    private void loadAuditLogs() {
        try {
            com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
            java.util.List<String> logs = context.getRmiProvider().getService().getAuditLogs(100, context.getSessionToken());
            auditListView.getItems().setAll(logs);
        } catch (Exception e) {
            statusLabel.setText("Failed to load audit logs: " + e.getMessage());
        }
    }

    @FXML
    private void handleCreateUser() {
        try {
            String u = usernameField.getText();
            String p = passwordField.getText();
            String r = roleCombo.getValue();
            
            com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
            context.getRmiProvider().getService().createUser(u, p, r, context.getSessionToken());
            statusLabel.setText("User created successfully");
            usernameField.clear(); passwordField.clear();
            loadUsers();
        } catch (Exception e) {
            statusLabel.setText("Creation failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleBackup() {
        try {
            com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
            byte[] db = context.getRmiProvider().getService().backupDatabase(context.getSessionToken());
            java.io.File file = new java.io.File("backup_" + System.currentTimeMillis() + ".db");
            java.nio.file.Files.write(file.toPath(), db);
            statusLabel.setText("Database backed up to " + file.getAbsolutePath());
        } catch (Exception e) {
            statusLabel.setText("Backup failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefreshLogs() {
        loadAuditLogs();
    }

    @FXML
    private void handleLogout() {
        try {
            com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
            context.getRmiProvider().getService().logout(context.getSessionToken());
            context.clearSession();
            context.getViewLoader().loadView("login.fxml");
        } catch (Exception e) {
            statusLabel.setText("Logout failed: " + e.getMessage());
        }
    }
}
