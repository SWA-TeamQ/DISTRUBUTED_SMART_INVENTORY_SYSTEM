package com.auction.client.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AdminPanelController {

    @FXML private javafx.scene.control.TableView<com.auction.shared.models.User> usersTable;
    @FXML private javafx.scene.control.ListView<String> auditListView;
    @FXML private javafx.scene.control.TextField searchField;
    @FXML private javafx.scene.control.TextField promoteField;
    @FXML private javafx.scene.control.Label statusLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label adminUsersLabel;
    @FXML private Label standardUsersLabel;
    @FXML private Label auditCountLabel;
    @FXML private Label lastUpdatedLabel;

    @FXML
    public void initialize() {
        refreshDashboard();
    }

    private void refreshDashboard() {
        try {
            com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
            var service = context.getRmiProvider().getService();

            java.util.List<com.auction.shared.models.User> users = service.getAllUsers(context.getSessionToken());
            usersTable.getItems().setAll(users);
            java.util.List<String> logs = service.getAuditLogs(100, context.getSessionToken());
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
        } catch (java.rmi.RemoteException e) {
            com.auction.client.core.ClientContext.getInstance().handleConnectionLost();
        } catch (Exception e) {
            statusLabel.setText("Refresh failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleSearchUser() {
        try {
            String query = searchField.getText();
            com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
            java.util.List<com.auction.shared.models.User> users;
            if (query == null || query.trim().isEmpty()) {
                users = context.getRmiProvider().getService().getAllUsers(context.getSessionToken());
            } else {
                users = context.getRmiProvider().getService().searchUsers(query, context.getSessionToken());
            }
            usersTable.getItems().setAll(users);
            statusLabel.setText("Search completed.");
        } catch (java.rmi.RemoteException e) {
            com.auction.client.core.ClientContext.getInstance().handleConnectionLost();
        } catch (Exception e) {
            statusLabel.setText("Search failed: " + e.getMessage());
        }
    }

    @FXML
    private void handlePromoteUser() {
        try {
            String username = promoteField.getText();
            com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
            context.getRmiProvider().getService().promoteUserToAdmin(username, context.getSessionToken());
            statusLabel.setText("User promoted to admin successfully");
            promoteField.clear();
            handleSearchUser(); // Refresh the table
        } catch (java.rmi.RemoteException e) {
            com.auction.client.core.ClientContext.getInstance().handleConnectionLost();
        } catch (Exception e) {
            statusLabel.setText("Promotion failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefreshLogs() {
        refreshDashboard();
    }

    @FXML
    private void handleRefreshDashboard() {
        refreshDashboard();
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
