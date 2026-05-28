package com.auction.client.controllers;

import com.auction.client.core.ClientContext;
import com.auction.client.service.ThumbnailExecutor;
import com.auction.shared.Constants;
import com.auction.shared.models.AuctionItem;
import com.auction.shared.models.User;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AdminPanelController {

    @FXML private TableView<User> usersTable;
    @FXML private ListView<String> auditListView;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label statusLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label adminUsersLabel;
    @FXML private Label standardUsersLabel;
    @FXML private Label auditCountLabel;
    @FXML private Label lastUpdatedLabel;
    @FXML private TextField userSearchField;
    @FXML private TableView<AuctionItem> auctionsTable;
    @FXML private TableColumn<AuctionItem, Number> adminBidColumn;

    private List<User> allUsers = List.of();
    private final Map<String, Image> thumbnailCache = new ConcurrentHashMap<>();
    private static final Image PLACEHOLDER = loadPlaceholder();

    @FXML
    public void initialize() {
        roleCombo.getItems().addAll(Constants.USER, Constants.ADMIN);
        roleCombo.getSelectionModel().selectFirst();

        configureAdminBidColumn();
        configureAuctionsTable();
        configureUserSearch();
        addThumbnailColumn(auctionsTable);

        refreshDashboard();
    }

    private void configureAdminBidColumn() {
        if (adminBidColumn == null) return;
        adminBidColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(Constants.formatCents(item.longValue()));
            }
        });
    }

    private void configureAuctionsTable() {
        if (auctionsTable == null) return;
        auctionsTable.setRowFactory(tv -> {
            TableRow<AuctionItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !row.isEmpty()) {
                    openAuctionDetail(row.getItem());
                }
            });
            return row;
        });
    }

    private void configureUserSearch() {
        if (userSearchField == null) return;
        userSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                usersTable.getItems().setAll(allUsers);
                return;
            }
            String q = newVal.trim().toLowerCase();
            List<User> filtered = allUsers.stream()
                .filter(u -> u != null && u.getUsername() != null && u.getUsername().toLowerCase().contains(q))
                .collect(Collectors.toList());
            usersTable.getItems().setAll(filtered);
        });
    }

    private void refreshDashboard() {
        try {
            ClientContext context = ClientContext.getInstance();
            var service = context.getRmiProvider().getService();
            String token = context.getSessionToken();

            List<User> users = service.getAllUsers(token);
            allUsers = users;
            String q = userSearchField == null ? null : userSearchField.getText();
            if (q != null && !q.isBlank()) {
                String ql = q.trim().toLowerCase();
                users = users.stream()
                    .filter(u -> u != null && u.getUsername() != null && u.getUsername().toLowerCase().contains(ql))
                    .collect(Collectors.toList());
            }
            usersTable.getItems().setAll(users);

            List<String> logs = service.getAuditLogs(100, token);
            auditListView.getItems().setAll(logs);

            long adminCount = allUsers.stream()
                .filter(user -> Constants.ADMIN.equals(user.getRoleType()))
                .count();
            long userCount = allUsers.size() - adminCount;

            totalUsersLabel.setText(String.valueOf(allUsers.size()));
            adminUsersLabel.setText(String.valueOf(adminCount));
            standardUsersLabel.setText(String.valueOf(userCount));
            auditCountLabel.setText(String.valueOf(logs.size()));
            lastUpdatedLabel.setText("Updated " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));

            statusLabel.setText("Dashboard refreshed successfully.");
        } catch (java.rmi.RemoteException e) {
            ClientContext.getInstance().handleConnectionLost();
        } catch (Exception e) {
            statusLabel.setText("Refresh failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefreshAuctions() {
        try {
            ClientContext context = ClientContext.getInstance();
            var service = context.getRmiProvider().getService();
            List<AuctionItem> auctions = service.getAllAuctions();
            auctionsTable.getItems().setAll(auctions);
            statusLabel.setText("Auctions refreshed (" + auctions.size() + " total).");
        } catch (java.rmi.RemoteException e) {
            ClientContext.getInstance().handleConnectionLost();
        } catch (Exception e) {
            statusLabel.setText("Failed to load auctions: " + e.getMessage());
        }
    }

    @FXML
    private void handleCreateUser() {
        try {
            String u = usernameField.getText();
            String p = passwordField.getText();
            String r = roleCombo.getValue();

            ClientContext context = ClientContext.getInstance();
            context.getRmiProvider().getService().createUser(u, p, r, context.getSessionToken());
            statusLabel.setText("User created successfully");
            usernameField.clear();
            passwordField.clear();
            refreshDashboard();
        } catch (java.rmi.RemoteException e) {
            ClientContext.getInstance().handleConnectionLost();
        } catch (Exception e) {
            statusLabel.setText("Creation failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleBackup() {
        try {
            ClientContext context = ClientContext.getInstance();
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
        refreshDashboard();
    }

    @FXML
    private void handleRefreshDashboard() {
        refreshDashboard();
    }

    @FXML
    private void handleLogout() {
        try {
            ClientContext context = ClientContext.getInstance();
            context.getRmiProvider().getService().logout(context.getSessionToken());
            context.clearSession();
            context.getViewLoader().loadView("login.fxml");
        } catch (Exception e) {
            statusLabel.setText("Logout failed: " + e.getMessage());
        }
    }

    private void openAuctionDetail(AuctionItem item) {
        if (item == null) return;
        try {
            ClientContext ctx = ClientContext.getInstance();
            ctx.setPreviousViewName("admin_panel.fxml");
            ctx.setCurrentAuctionId(item.getId());
            AuctionDetailController controller = ctx.getViewLoader().<AuctionDetailController>loadView("auction_detail.fxml");
            controller.setReturnViewName("admin_panel.fxml");
        } catch (Exception e) {
            statusLabel.setText("Open detail failed: " + e.getMessage());
        }
    }

    private static Image loadPlaceholder() {
        try (var stream = AdminPanelController.class.getResourceAsStream("/images/placeholder.png")) {
            return stream == null ? null : new Image(stream);
        } catch (IOException e) {
            return null;
        }
    }

    private void loadThumbnailAsync(int auctionId, int index, ImageView target) {
        String key = auctionId + ":" + index;
        Image cached = thumbnailCache.get(key);
        if (cached != null) {
            target.setImage(cached);
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                byte[] bytes = ClientContext.getInstance()
                    .getRmiProvider()
                    .getService()
                    .getThumbnail(auctionId, index);
                return (bytes == null || bytes.length == 0) ? null : new Image(new ByteArrayInputStream(bytes));
            } catch (Exception e) {
                return null;
            }
        }, ThumbnailExecutor.getExecutor()).thenAccept(image -> {
            Image finalImage = image == null ? PLACEHOLDER : image;
            thumbnailCache.put(key, finalImage);
            Platform.runLater(() -> target.setImage(finalImage));
        });
    }

    private void addThumbnailColumn(TableView<AuctionItem> table) {
        if (table == null) return;
        TableColumn<AuctionItem, AuctionItem> thumbCol = new TableColumn<>("");
        thumbCol.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        thumbCol.setPrefWidth(56);
        thumbCol.setCellFactory(col -> new TableCell<>() {
            private final ImageView imageView = new ImageView();
            {
                imageView.setFitWidth(48);
                imageView.setFitHeight(48);
                imageView.setPreserveRatio(true);
                imageView.getStyleClass().add("image-thumb");
            }
            @Override
            protected void updateItem(AuctionItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                loadThumbnailAsync(item.getId(), 0, imageView);
                setGraphic(imageView);
            }
        });
        table.getColumns().add(0, thumbCol);
    }
}
