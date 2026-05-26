package com.auction.client.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.kordamp.ikonli.javafx.FontIcon;

public class AdminPanelController {

    @FXML private VBox contentArea;
    @FXML private Label statusLabel;
    
    // UI components initialized in initUsersView
    private VBox usersView;
    private TableView<com.auction.shared.models.User> usersTable;
    private TextField searchField;
    private ObservableList<com.auction.shared.models.User> allUsers = FXCollections.observableArrayList();
    
    // KPI Labels
    private Label totalUsersVal = new Label("0");
    private Label adminUsersVal = new Label("0");
    private Label standardUsersVal = new Label("0");

    @FXML
    public void initialize() {
        initUsersView();
        showUsers();
    }

    private void initUsersView() {
        usersView = new VBox(20);
        usersView.setPadding(new javafx.geometry.Insets(24));
        
        // KPI Section - Use GridPane for better spacing
        GridPane kpis = new GridPane();
        kpis.setHgap(16);
        kpis.setVgap(16);
        kpis.setPrefWidth(Double.MAX_VALUE);
        
        VBox totalCard = createMetricCard("Total Users", totalUsersVal);
        VBox adminsCard = createMetricCard("Admins", adminUsersVal);
        VBox usersCard = createMetricCard("Users", standardUsersVal);
        
        GridPane.setHgrow(totalCard, Priority.ALWAYS);
        GridPane.setHgrow(adminsCard, Priority.ALWAYS);
        GridPane.setHgrow(usersCard, Priority.ALWAYS);
        
        kpis.add(totalCard, 0, 0);
        kpis.add(adminsCard, 1, 0);
        kpis.add(usersCard, 2, 0);
        
        // Search Section with inline filtering
        searchField = new TextField();
        searchField.setPromptText("Search by username...");
        searchField.getStyleClass().add("compact-field");
        searchField.setStyle("-fx-min-height: 36px; -fx-font-size: 13px;");
        
        usersTable = new TableView<>();
        usersTable.getStyleClass().add("users-table");
        VBox.setVgrow(usersTable, Priority.ALWAYS);
        
        TableColumn<com.auction.shared.models.User, String> nameCol = new TableColumn<>("Username");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        nameCol.setPrefWidth(200);
        
        TableColumn<com.auction.shared.models.User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("roleType"));
        roleCol.setPrefWidth(100);
        
        TableColumn<com.auction.shared.models.User, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button();
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    var user = getTableView().getItems().get(getIndex());
                    var adminUser = com.auction.client.core.ClientContext.getInstance().getUsername();
                    
                    if (user.getUsername().equals(adminUser)) {
                        btn.setText("Self");
                        btn.setDisable(true);
                        btn.setStyle("-fx-padding: 6px 12px; -fx-font-size: 12px;");
                    } else if ("ADMIN".equals(user.getRoleType())) {
                        btn.setText("Demote");
                        btn.getStyleClass().add("action-demote");
                        btn.setStyle("-fx-padding: 6px 12px; -fx-font-size: 12px; -fx-background-color: transparent; -fx-border-color: #da3633; -fx-border-width: 1; -fx-text-fill: #f85149; -fx-border-radius: 6; -fx-cursor: hand;");
                        btn.setOnAction(e -> demoteUser(user.getUsername()));
                    } else {
                        btn.setText("Promote");
                        btn.getStyleClass().add("action-promote");
                        btn.setStyle("-fx-padding: 6px 12px; -fx-font-size: 12px; -fx-background-color: transparent; -fx-border-color: #2da44e; -fx-border-width: 1; -fx-text-fill: #3fb950; -fx-border-radius: 6; -fx-cursor: hand;");
                        btn.setOnAction(e -> promoteUser(user.getUsername()));
                    }
                    setGraphic(btn);
                }
            }
        });
        
        usersTable.getColumns().addAll(nameCol, roleCol, actionCol);
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        usersTable.setItems(allUsers);
        
        // Add real-time search filtering
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterUsers(newVal));
        
        HBox header = new HBox(10, searchField);
        header.setPrefHeight(36);
        header.setStyle("-fx-alignment: CENTER_LEFT;");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        
        usersView.getChildren().addAll(kpis, header, usersTable);
    }

    private VBox createMetricCard(String title, Label value) {
        VBox card = new VBox(8);
        card.getStyleClass().add("metric-card");
        Label t = new Label(title);
        t.getStyleClass().add("metric-label");
        value.getStyleClass().add("metric-value");
        card.getChildren().addAll(t, value);
        return card;
    }

    private void filterUsers(String query) {
        if (query == null || query.isEmpty()) {
            usersTable.setItems(allUsers);
        } else {
            String lowerQuery = query.toLowerCase();
            ObservableList<com.auction.shared.models.User> filtered = allUsers.filtered(user ->
                user.getUsername().toLowerCase().contains(lowerQuery)
            );
            usersTable.setItems(filtered);
        }
    }

    @FXML public void showUsers() { contentArea.getChildren().setAll(usersView); refreshUsers(); }
    @FXML public void showAuctions() { contentArea.getChildren().setAll(new VBox(15, new Label("Auction Management Placeholder"))); }
    @FXML public void showLogs() { contentArea.getChildren().setAll(new VBox(15, new Label("Audit Log Placeholder"))); }
    @FXML public void handleRefreshDashboard() { refreshUsers(); }

    private void refreshUsers() {
        try {
            var context = com.auction.client.core.ClientContext.getInstance();
            var users = context.getRmiProvider().getService().getAllUsers(context.getSessionToken());
            allUsers.setAll(users);
            
            long adminCount = users.stream().filter(u -> "ADMIN".equals(u.getRoleType())).count();
            totalUsersVal.setText(String.valueOf(users.size()));
            adminUsersVal.setText(String.valueOf(adminCount));
            standardUsersVal.setText(String.valueOf(users.size() - adminCount));
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void promoteUser(String username) {
        try {
            var context = com.auction.client.core.ClientContext.getInstance();
            context.getRmiProvider().getService().promoteUserToAdmin(username, context.getSessionToken());
            refreshUsers();
        } catch (Exception e) {
            statusLabel.setText("Promotion failed: " + e.getMessage());
        }
    }

    private void demoteUser(String username) {
        try {
            var context = com.auction.client.core.ClientContext.getInstance();
            // Demotion by promoting to User (assuming same endpoint or specific logic exists)
            context.getRmiProvider().getService().promoteUserToAdmin(username, context.getSessionToken());
            refreshUsers();
        } catch (Exception e) {
            statusLabel.setText("Demotion failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        try {
            var context = com.auction.client.core.ClientContext.getInstance();
            context.getRmiProvider().getService().logout(context.getSessionToken());
            context.clearSession();
            context.getViewLoader().loadView("login.fxml");
        } catch (Exception e) {
            statusLabel.setText("Logout failed: " + e.getMessage());
        }
    }
}
