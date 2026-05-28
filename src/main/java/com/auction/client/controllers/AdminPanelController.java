package com.auction.client.controllers;

import atlantafx.base.theme.Styles;
import com.auction.client.core.ClientContext;
import com.auction.client.service.ThumbnailExecutor;
import com.auction.client.util.Toast;
import com.auction.shared.Constants;
import com.auction.shared.exceptions.AuctionException;
import com.auction.shared.interfaces.IAuctionService;
import com.auction.shared.models.AuctionItem;
import com.auction.shared.models.User;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

@SuppressWarnings("unused")
public class AdminPanelController {

  private enum Section {
    USERS,
    AUCTIONS,
    LOGS,
  }

  @FXML
  private VBox contentArea;

  @FXML
  private TextField usernameField;

  @FXML
  private PasswordField passwordField;

  @FXML
  private ComboBox<String> roleCombo;

  @FXML
  private Label statusLabel;

  @FXML
  private Label lastUpdatedLabel;

  private Section currentSection = Section.USERS;
  private List<User> allUsers = List.of();
  private List<AuctionItem> allAuctions = List.of();
  private TextField userSearchField;
  private TableView<User> usersTable;
  private TableView<AuctionItem> auctionsTable;
  private ListView<String> auditListView;
  private final Map<String, Image> thumbnailCache = new ConcurrentHashMap<>();
  private static final Image PLACEHOLDER = loadPlaceholder();

  @FXML
  public void initialize() {
    if (roleCombo != null) {
      roleCombo.getItems().setAll(Constants.USER, Constants.ADMIN);
      roleCombo.getSelectionModel().selectFirst();
    }
    showUsers();
  }

  @FXML
  private void showUsers() {
    currentSection = Section.USERS;
    try {
      IAuctionService service = requireService();
      List<User> users = safeUsers(
        service.getAllUsers(ClientContext.getInstance().getSessionToken())
      );
      allUsers = users;
      renderUsersView();
      updateStatus("Users loaded successfully.");
      updateLastUpdated();
    } catch (AuctionException | java.rmi.RemoteException e) {
      if (e instanceof java.rmi.RemoteException) {
        ClientContext.getInstance().handleConnectionLost();
      } else {
        showError("Failed to load users", (AuctionException) e);
      }
    }
  }

  @FXML
  private void showAuctions() {
    currentSection = Section.AUCTIONS;
    try {
      IAuctionService service = requireService();
      List<AuctionItem> auctions = safeAuctions(service.getAllAuctions());
      allAuctions = auctions;
      renderAuctionsView();
      updateStatus("Auctions loaded successfully.");
      updateLastUpdated();
    } catch (AuctionException | java.rmi.RemoteException e) {
      if (e instanceof java.rmi.RemoteException) {
        ClientContext.getInstance().handleConnectionLost();
      } else {
        showError("Failed to load auctions", (AuctionException) e);
      }
    }
  }

  @FXML
  private void showLogs() {
    currentSection = Section.LOGS;
    try {
      IAuctionService service = requireService();
      List<String> logs = service.getAuditLogs(
        200,
        ClientContext.getInstance().getSessionToken()
      );
      renderLogsView(logs == null ? List.of() : logs);
      updateStatus("Audit logs loaded successfully.");
      updateLastUpdated();
    } catch (AuctionException | java.rmi.RemoteException e) {
      if (e instanceof java.rmi.RemoteException) {
        ClientContext.getInstance().handleConnectionLost();
      } else {
        showError("Failed to load logs", (AuctionException) e);
      }
    }
  }

  @FXML
  private void handleRefreshDashboard() {
    refreshCurrentSection();
  }

  @FXML
  private void handleRefreshUsers() {
    showUsers();
  }

  @FXML
  private void handleRefreshAuctions() {
    showAuctions();
  }

  @FXML
  private void handleRefreshLogs() {
    showLogs();
  }

  @FXML
  private void handleCreateUser() {
    try {
      String username = usernameField == null ? null : usernameField.getText();
      String password = passwordField == null ? null : passwordField.getText();
      String role = roleCombo == null ? null : roleCombo.getValue();

      requireService().createUser(
        username,
        password,
        role,
        ClientContext.getInstance().getSessionToken()
      );
      if (usernameField != null) {
        usernameField.clear();
      }
      if (passwordField != null) {
        passwordField.clear();
      }
      updateStatus("User created successfully.");
      refreshCurrentSection();
    } catch (AuctionException | java.rmi.RemoteException e) {
      if (e instanceof java.rmi.RemoteException) {
        ClientContext.getInstance().handleConnectionLost();
      } else {
        showError("Creation failed", (AuctionException) e);
      }
    }
  }

  @FXML
  private void handleBackup() {
    try {
      byte[] db = requireService().backupDatabase(
        ClientContext.getInstance().getSessionToken()
      );
      java.io.File file = new java.io.File(
        "backup_" + System.currentTimeMillis() + ".db"
      );
      java.nio.file.Files.write(file.toPath(), db);
      updateStatus("Database backed up to " + file.getAbsolutePath());
    } catch (AuctionException | java.rmi.RemoteException e) {
      if (e instanceof java.rmi.RemoteException) {
        ClientContext.getInstance().handleConnectionLost();
      } else {
        showError("Backup failed", (AuctionException) e);
      }
    } catch (IOException e) {
      showError("Backup failed", e);
    }
  }

  @FXML
  private void handleLogout() {
    try {
      ClientContext context = ClientContext.getInstance();
      IAuctionService service = context.getRmiProvider().getService();
      if (service != null) {
        service.logout(context.getSessionToken());
      }
      context.clearSession();
      context.getViewLoader().loadView("login.fxml");
    } catch (java.rmi.RemoteException e) {
      ClientContext.getInstance().handleConnectionLost();
    } catch (IOException e) {
      showError("Logout failed", e);
    }
  }

  private IAuctionService requireService() throws AuctionException {
    IAuctionService service = ClientContext.getInstance()
      .getRmiProvider()
      .getService();
    if (service == null) {
      throw new AuctionException(
        "Admin panel is not connected to the auction service"
      );
    }
    return service;
  }

  private void refreshCurrentSection() {
    switch (currentSection) {
      case USERS -> showUsers();
      case AUCTIONS -> showAuctions();
      case LOGS -> showLogs();
    }
  }

  private void renderUsersView() {
    if (contentArea == null) {
      return;
    }

    userSearchField = new TextField();
    userSearchField.setPromptText("Search users...");
    userSearchField.getStyleClass().add("compact-field");
    userSearchField.setText("");
    userSearchField
      .textProperty()
      .addListener((obs, oldValue, newValue) -> applyUserFilter());

    Button refreshButton = new Button("Refresh");
    refreshButton.getStyleClass().add("secondary-button");
    refreshButton.setOnAction(event -> showUsers());

    usersTable = buildUsersTable();
    applyUserFilter();

    long adminCount = allUsers
      .stream()
      .filter(
        user -> user != null && Constants.ADMIN.equals(user.getRoleType())
      )
      .count();
    long userCount = Math.max(0L, allUsers.size() - adminCount);

    VBox usersCard = new VBox(12);
    usersCard.getStyleClass().add("panel-card");

    HBox toolbar = new HBox(12);
    toolbar.getStyleClass().add("content-toolbar");
    VBox textGroup = new VBox(2);
    textGroup
      .getChildren()
      .addAll(
        buildSectionTitle("All Users"),
        buildSectionCopy(
          "Usernames, roles, and created timestamps from the live admin service."
        )
      );
    HBox toolbarSpacer = new HBox();
    HBox.setHgrow(toolbarSpacer, javafx.scene.layout.Priority.ALWAYS);
    toolbar
      .getChildren()
      .addAll(textGroup, toolbarSpacer, userSearchField, refreshButton);

    usersCard.getChildren().addAll(toolbar, usersTable);

    FlowPane metrics = new FlowPane(14, 14);
    metrics
      .getChildren()
      .addAll(
        buildMetricCard(
          "Total Users",
          String.valueOf(allUsers.size()),
          false,
          false
        ),
        buildMetricCard("Admins", String.valueOf(adminCount), true, false),
        buildMetricCard("Users", String.valueOf(userCount), false, false)
      );

    contentArea
      .getChildren()
      .setAll(
        buildHeaderCard(
          "Users",
          "All registered accounts managed through the current admin service."
        ),
        metrics,
        usersCard
      );
  }

  private void renderAuctionsView() {
    if (contentArea == null) {
      return;
    }

    auctionsTable = buildAuctionsTable(safeAuctions(allAuctions));

    Button refreshButton = new Button("Refresh Auctions");
    refreshButton.getStyleClass().add("secondary-button");
    refreshButton.setOnAction(event -> showAuctions());

    VBox auctionsCard = new VBox(12);
    auctionsCard.getStyleClass().add("panel-card");

    HBox toolbar = new HBox(12);
    toolbar.getStyleClass().add("content-toolbar");
    VBox textGroup = new VBox(2);
    textGroup
      .getChildren()
      .addAll(
        buildSectionTitle("All Auctions"),
        buildSectionCopy(
          "Browse every auction, open details, and manage listings with the existing backend actions."
        )
      );
    HBox toolbarSpacer = new HBox();
    HBox.setHgrow(toolbarSpacer, javafx.scene.layout.Priority.ALWAYS);
    toolbar.getChildren().addAll(textGroup, toolbarSpacer, refreshButton);

    long activeCount = allAuctions
      .stream()
      .filter(
        item ->
          item != null &&
          Constants.STATUS_ACTIVE.equalsIgnoreCase(item.getStatus())
      )
      .count();
    long closedCount = allAuctions.size() - activeCount;

    FlowPane metrics = new FlowPane(14, 14);
    metrics
      .getChildren()
      .addAll(
        buildMetricCard(
          "Total Auctions",
          String.valueOf(allAuctions.size()),
          false,
          false
        ),
        buildMetricCard("Active", String.valueOf(activeCount), true, false),
        buildMetricCard(
          "Closed",
          String.valueOf(Math.max(0L, closedCount)),
          false,
          true
        )
      );

    auctionsCard.getChildren().addAll(toolbar, auctionsTable);

    contentArea
      .getChildren()
      .setAll(
        buildHeaderCard(
          "Auctions",
          "Current auction inventory shown with thumbnails and action menus."
        ),
        metrics,
        auctionsCard
      );
  }

  private void renderLogsView(List<String> logs) {
    if (contentArea == null) {
      return;
    }

    auditListView = new ListView<>();
    auditListView.getStyleClass().add("audit-list");
    auditListView.getItems().setAll(logs);
    auditListView.setPlaceholder(new Label("No audit logs available."));

    Button refreshButton = new Button("Refresh Logs");
    refreshButton.getStyleClass().add("secondary-button");
    refreshButton.setOnAction(event -> showLogs());

    VBox logsCard = new VBox(12);
    logsCard.getStyleClass().add("panel-card");

    HBox toolbar = new HBox(12);
    toolbar.getStyleClass().add("content-toolbar");
    VBox textGroup = new VBox(2);
    textGroup
      .getChildren()
      .addAll(
        buildSectionTitle("Audit Logs"),
        buildSectionCopy(
          "Recent administrative and system events pulled from the live audit trail."
        )
      );
    HBox toolbarSpacer = new HBox();
    HBox.setHgrow(toolbarSpacer, javafx.scene.layout.Priority.ALWAYS);
    toolbar.getChildren().addAll(textGroup, toolbarSpacer, refreshButton);

    FlowPane metrics = new FlowPane(14, 14);
    metrics
      .getChildren()
      .addAll(
        buildMetricCard("Log Lines", String.valueOf(logs.size()), false, false),
        buildMetricCard(
          "Last Refresh",
          LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
          true,
          false
        )
      );

    logsCard.getChildren().addAll(toolbar, auditListView);

    contentArea
      .getChildren()
      .setAll(
        buildHeaderCard(
          "Audit Logs",
          "Operational trace and security events from the current session."
        ),
        metrics,
        logsCard
      );
  }

  private VBox buildHeaderCard(String title, String subtitle) {
    VBox header = new VBox(4);
    header.getStyleClass().add("panel-card");

    Label titleLabel = buildSectionTitle(title);
    Label subtitleLabel = buildSectionCopy(subtitle);
    subtitleLabel.setWrapText(true);

    header.getChildren().addAll(titleLabel, subtitleLabel);
    return header;
  }

  private Label buildSectionTitle(String text) {
    Label label = new Label(text);
    label.getStyleClass().add("section-title");
    return label;
  }

  private Label buildSectionCopy(String text) {
    Label label = new Label(text);
    label.getStyleClass().add("section-copy");
    return label;
  }

  private VBox buildMetricCard(
    String labelText,
    String valueText,
    boolean accent,
    boolean warm
  ) {
    VBox card = new VBox(4);
    card.getStyleClass().add("metric-card");
    if (accent) {
      card.getStyleClass().add("metric-card-accent");
    }
    if (warm) {
      card.getStyleClass().add("metric-card-warm");
    }

    Label label = new Label(labelText);
    label.getStyleClass().add("metric-label");

    Label value = new Label(valueText);
    value.getStyleClass().add("metric-value");

    card.getChildren().addAll(label, value);
    return card;
  }

  private TableView<User> buildUsersTable() {
    TableView<User> table = new TableView<>();
    table.getStyleClass().add("users-table");
    table.setPlaceholder(new Label("No users loaded yet."));

    TableColumn<User, String> usernameCol = new TableColumn<>("Username");
    usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
    usernameCol.setPrefWidth(180);

    TableColumn<User, String> roleCol = new TableColumn<>("Role");
    roleCol.setCellValueFactory(new PropertyValueFactory<>("roleType"));
    roleCol.setPrefWidth(120);

    TableColumn<User, String> createdCol = new TableColumn<>("Created At");
    createdCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
    createdCol.setPrefWidth(220);

    table.getColumns().add(usernameCol);
    table.getColumns().add(roleCol);
    table.getColumns().add(createdCol);
    return table;
  }

  private TableView<AuctionItem> buildAuctionsTable(
    List<AuctionItem> auctions
  ) {
    TableView<AuctionItem> table = new TableView<>();
    table.getStyleClass().add("auction-table");
    table.setPlaceholder(new Label("No auctions loaded yet."));
    table.setRowFactory(tv -> {
      TableRow<AuctionItem> row = new TableRow<>();
      row.setOnMouseClicked(event -> {
        if (
          event.getButton() == MouseButton.PRIMARY &&
          event.getClickCount() == 2 &&
          !row.isEmpty()
        ) {
          openAuctionDetail(row.getItem());
        }
      });
      return row;
    });

    TableColumn<AuctionItem, AuctionItem> thumbCol = new TableColumn<>("");
    thumbCol.setCellValueFactory(param ->
      new ReadOnlyObjectWrapper<>(param.getValue())
    );
    thumbCol.setPrefWidth(56);
    thumbCol.setCellFactory(col ->
      new TableCell<>() {
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
      }
    );

    TableColumn<AuctionItem, Integer> idCol = new TableColumn<>("ID");
    idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
    idCol.setPrefWidth(70);

    TableColumn<AuctionItem, String> titleCol = new TableColumn<>("Title");
    titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
    titleCol.setPrefWidth(220);

    TableColumn<AuctionItem, String> sellerCol = new TableColumn<>("Seller");
    sellerCol.setCellValueFactory(new PropertyValueFactory<>("sellerUsername"));
    sellerCol.setPrefWidth(150);

    TableColumn<AuctionItem, String> statusCol = new TableColumn<>("Status");
    statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
    statusCol.setPrefWidth(110);

    TableColumn<AuctionItem, Number> bidCol = new TableColumn<>("Current Bid");
    bidCol.setCellValueFactory(new PropertyValueFactory<>("currentBidCents"));
    bidCol.setPrefWidth(130);
    bidCol.setCellFactory(col ->
      new TableCell<>() {
        @Override
        protected void updateItem(Number item, boolean empty) {
          super.updateItem(item, empty);
          setText(
            empty || item == null
              ? null
              : Constants.formatCents(item.longValue())
          );
        }
      }
    );

    TableColumn<AuctionItem, AuctionItem> actionsCol = new TableColumn<>(
      "Actions"
    );
    actionsCol.setCellValueFactory(param ->
      new ReadOnlyObjectWrapper<>(param.getValue())
    );
    actionsCol.setPrefWidth(150);
    actionsCol.setCellFactory(col ->
      new TableCell<>() {
        private final MenuButton menuButton = new MenuButton("Actions");

        {
          menuButton.getStyleClass().add(Styles.FLAT);
          menuButton.getStyleClass().add("action-menu");
          menuButton.setMaxWidth(Double.MAX_VALUE);
        }

        @Override
        protected void updateItem(AuctionItem item, boolean empty) {
          super.updateItem(item, empty);
          if (empty || item == null) {
            setGraphic(null);
            return;
          }
          menuButton.getItems().setAll(buildActionMenu(item));
          setGraphic(menuButton);
        }
      }
    );

    table.getColumns().add(thumbCol);
    table.getColumns().add(idCol);
    table.getColumns().add(titleCol);
    table.getColumns().add(sellerCol);
    table.getColumns().add(statusCol);
    table.getColumns().add(bidCol);
    table.getColumns().add(actionsCol);
    table.getItems().setAll(auctions);
    return table;
  }

  private List<MenuItem> buildActionMenu(AuctionItem item) {
    MenuItem viewDetails = new MenuItem("View Details");
    viewDetails.setOnAction(event -> openAuctionDetail(item));

    if (ownsListing(item) && isActive(item)) {
      MenuItem cancelListing = new MenuItem("Cancel Listing");
      cancelListing.setOnAction(event ->
        performAuctionAction(item, this::cancelAuction)
      );
      return List.of(viewDetails, cancelListing);
    }

    if (ownsListing(item) && isRelistable(item)) {
      MenuItem relist = new MenuItem("Relist");
      relist.setOnAction(event ->
        performAuctionAction(item, this::relistAuction)
      );
      return List.of(viewDetails, relist);
    }

    return List.of(viewDetails);
  }

  private boolean ownsListing(AuctionItem item) {
    String username = ClientContext.getInstance().getUsername();
    return (
      item != null &&
      username != null &&
      username.equalsIgnoreCase(item.getSellerUsername())
    );
  }

  private boolean isActive(AuctionItem item) {
    return (
      item != null &&
      item.getStatus() != null &&
      Constants.STATUS_ACTIVE.equalsIgnoreCase(item.getStatus())
    );
  }

  private boolean isRelistable(AuctionItem item) {
    if (item == null || item.getStatus() == null) {
      return false;
    }
    String status = item.getStatus().trim().toUpperCase(Locale.ROOT);
    return (
      status.equals(Constants.STATUS_EXPIRED) ||
      status.equals(Constants.STATUS_CANCELLED) ||
      status.equals("ENDED")
    );
  }

  private void performAuctionAction(AuctionItem item, AuctionAction action) {
    if (item == null) {
      return;
    }

    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Confirm Action");
    alert.setHeaderText(item.getTitle());
    alert.setContentText("Proceed with this auction action?");
    ButtonType proceed = new ButtonType("Proceed", ButtonBar.ButtonData.YES);
    ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.NO);
    alert.getButtonTypes().setAll(proceed, cancel);

    Optional<ButtonType> result = alert.showAndWait();
    if (result.isPresent() && result.get() == proceed) {
      try {
        action.execute(item);
        showAuctions();
      } catch (Exception e) {
        showError("Action failed: " + e.getMessage(), e);
      }
    }
  }

  private void cancelAuction(AuctionItem item) throws Exception {
    ClientContext ctx = ClientContext.getInstance();
    ctx
      .getRmiProvider()
      .getService()
      .cancelAuction(item.getId(), ctx.getSessionToken());
  }

  private void relistAuction(AuctionItem item) throws Exception {
    ClientContext ctx = ClientContext.getInstance();
    java.time.Instant newEnd = java.time.Instant.now().plus(
      java.time.Duration.ofDays(1)
    );
    ctx
      .getRmiProvider()
      .getService()
      .relistAuction(item.getId(), newEnd.toString(), ctx.getSessionToken());
  }

  private void openAuctionDetail(AuctionItem item) {
    if (item == null) {
      return;
    }

    try {
      ClientContext ctx = ClientContext.getInstance();
      ctx.setPreviousViewName("admin_panel.fxml");
      ctx.setCurrentAuctionId(item.getId());
      AuctionDetailController controller = ctx
        .getViewLoader()
        .<AuctionDetailController>loadView("auction_detail.fxml");
      controller.setReturnViewName("admin_panel.fxml");
    } catch (IOException | RuntimeException e) {
      showError("Open detail failed: " + e.getMessage(), e);
    }
  }

  private void applyUserFilter() {
    if (usersTable == null) {
      return;
    }

    String query = userSearchField == null ? "" : userSearchField.getText();
    if (query == null || query.isBlank()) {
      usersTable.getItems().setAll(allUsers);
      return;
    }

    String needle = query.trim().toLowerCase(Locale.ROOT);
    List<User> filtered = allUsers
      .stream()
      .filter(
        user ->
          user != null &&
          user.getUsername() != null &&
          user.getUsername().toLowerCase(Locale.ROOT).contains(needle)
      )
      .collect(Collectors.toList());
    usersTable.getItems().setAll(filtered);
  }

  private void loadThumbnailAsync(int auctionId, int index, ImageView target) {
    String key = auctionId + ":" + index;
    Image cached = thumbnailCache.get(key);
    if (cached != null) {
      target.setImage(cached);
      return;
    }

    CompletableFuture.supplyAsync(
      () -> {
        try {
          byte[] bytes = ClientContext.getInstance()
            .getRmiProvider()
            .getService()
            .getThumbnail(auctionId, index);
          return (bytes == null || bytes.length == 0)
            ? null
            : new Image(new ByteArrayInputStream(bytes));
        } catch (java.rmi.RemoteException | RuntimeException e) {
          return null;
        }
      },
      ThumbnailExecutor.getExecutor()
    ).thenAccept(image -> {
      Image finalImage = image == null ? PLACEHOLDER : image;
      thumbnailCache.put(key, finalImage);
      Platform.runLater(() -> target.setImage(finalImage));
    });
  }

  private static Image loadPlaceholder() {
    try (
      var stream = AdminPanelController.class.getResourceAsStream(
        "/images/placeholder.png"
      )
    ) {
      return stream == null ? null : new Image(stream);
    } catch (IOException e) {
      return null;
    }
  }

  private List<User> safeUsers(List<User> users) {
    List<User> list = users == null ? List.of() : new ArrayList<>(users);
    list.removeIf(user -> user == null);
    return list;
  }

  private List<AuctionItem> safeAuctions(List<AuctionItem> auctions) {
    List<AuctionItem> list =
      auctions == null ? List.of() : new ArrayList<>(auctions);
    list.removeIf(item -> item == null);
    list.sort(Comparator.comparingInt(AuctionItem::getId));
    return list;
  }

  private void updateStatus(String message) {
    if (statusLabel != null) {
      statusLabel.setText(message);
    }
  }

  private void updateLastUpdated() {
    if (lastUpdatedLabel != null) {
      lastUpdatedLabel.setText(
        "Updated " +
          LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
      );
    }
  }

  private void showError(String message, Exception error) {
    if (statusLabel != null) {
      statusLabel.setText(message);
    }
    if (contentArea != null) {
      contentArea.getChildren().setAll(buildHeaderCard("Admin Panel", message));
    }
    Toast.show(contentArea, message, Toast.Type.ERROR);
    if (error != null) {
      System.err.println(message + ": " + error.getMessage());
    }
  }

  private interface AuctionAction {
    void execute(AuctionItem item) throws Exception;
  }
}
