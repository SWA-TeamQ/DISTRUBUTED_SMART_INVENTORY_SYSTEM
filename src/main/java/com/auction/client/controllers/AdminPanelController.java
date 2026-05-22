package com.auction.client.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AdminPanelController {

    @FXML private javafx.scene.control.TableView<com.auction.shared.models.User> usersTable;
    @FXML private javafx.scene.control.ListView<String> auditListView;
    @FXML private javafx.scene.control.TextField usernameField;
    @FXML private javafx.scene.control.PasswordField passwordField;
    @FXML private javafx.scene.control.ComboBox<String> roleCombo;
    @FXML private javafx.scene.control.Label statusLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label adminUsersLabel;
    @FXML private Label standardUsersLabel;
    @FXML private Label auditCountLabel;
    @FXML private Label lastUpdatedLabel;

    @FXML
    public void initialize() {
        roleCombo.getItems().addAll(
            com.auction.shared.Constants.USER, 
            com.auction.shared.Constants.ADMIN
        );
        roleCombo.getSelectionModel().selectFirst();

        if (statusLabel != null) {
            statusLabel.setText("Loading dashboard...");
        }
        refreshDashboardAsync();
    }

    private void refreshDashboardAsync() {
        Thread loader = new Thread(() -> {
            try {
                com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
                var service = context.getRmiProvider().getService();

                java.util.List<com.auction.shared.models.User> users = service.getAllUsers(context.getSessionToken());
                java.util.List<String> logs = service.getAuditLogs(100, context.getSessionToken());

                javafx.application.Platform.runLater(() -> {
                    usersTable.getItems().setAll(users);
                    auditListView.getItems().setAll(logs);

                    long adminCount = users.stream()
                        .filter(user -> com.auction.shared.Constants.ADMIN.equals(user.getRoleType()))
                        .count();
                    long userCount = users.size() - adminCount;

                    totalUsersLabel.setText(String.valueOf(users.size()));
                    adminUsersLabel.setText(String.valueOf(adminCount));
                    standardUsersLabel.setText(String.valueOf(userCount));
                    auditCountLabel.setText(String.valueOf(logs.size()));
                    lastUpdatedLabel.setText("Updated " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));

                    statusLabel.setText("Dashboard refreshed successfully.");
                });
            } catch (java.rmi.RemoteException e) {
                javafx.application.Platform.runLater(() -> com.auction.client.core.ClientContext.getInstance().handleConnectionLost());
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    if (statusLabel != null) {
                        statusLabel.setText("Refresh failed: " + e.getMessage());
                    }
                });
            }
        }, "AdminDashboardLoader");
        loader.setDaemon(true);
        loader.start();
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
            usernameField.clear();
            passwordField.clear();
            refreshDashboardAsync();
        } catch (java.rmi.RemoteException e) {
            com.auction.client.core.ClientContext.getInstance().handleConnectionLost();
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
            refreshDashboardAsync();
    }

    @FXML
    private void handleRefreshDashboard() {
        refreshDashboardAsync();
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
