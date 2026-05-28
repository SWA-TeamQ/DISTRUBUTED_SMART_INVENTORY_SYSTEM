package com.auction.client.controllers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.auction.client.core.ClientContext;
import com.auction.client.service.ThumbnailExecutor;
import com.auction.client.state.AuctionUiState;
import com.auction.shared.Constants;
import com.auction.shared.exceptions.AuctionException;
import com.auction.shared.interfaces.IAuctionService;
import com.auction.shared.models.AuctionItem;
import com.auction.shared.models.User;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;

public class AdminPanelController {

  @FXML
  private VBox contentArea;

  @FXML
  public void initialize() {
    showUsers();
  }

  @FXML
  private void showUsers() {
    try {
      IAuctionService service = getService();
      List<User> users = service.getAllUsers(
        ClientContext.getInstance().getSessionToken()
      );
      contentArea
        .getChildren()
        .setAll(
          buildHeader("Users", "All registered users in the system."),
          buildUsersTable(users)
        );
    } catch (Exception e) {
      showError("Failed to load users: " + e.getMessage());
    }
  }

  @FXML
  private void showAuctions() {
    try {
      IAuctionService service = getService();
      List<AuctionItem> auctions = service.getActiveAuctions();

      // Sync server-provided auctions into the shared client UI state
      ObservableList<AuctionItem> shared =
        AuctionUiState.getInstance().getActiveAuctions();
      shared.setAll(auctions);

      contentArea
        .getChildren()
        .setAll(
          buildHeader(
            "Auctions",
            "Active auctions visible to the admin account."
          ),
          buildAuctionsTable(shared)
        );
    } catch (Exception e) {
      showError("Failed to load auctions: " + e.getMessage());
    }
  }

  @FXML
  private void showLogs() {
    try {
      IAuctionService service = getService();
      List<String> logs = service.getAuditLogs(
        200,
        ClientContext.getInstance().getSessionToken()
      );
      VBox logsBox = new VBox(8);
      logsBox.getStyleClass().add("panel-card");
      logsBox
        .getChildren()
        .add(
          buildHeader(
            "Audit Logs",
            "Most recent administrative and system events."
          )
        );

      if (logs.isEmpty()) {
        logsBox.getChildren().add(new Label("No audit logs available."));
      } else {
        for (String line : logs) {
          Label entry = new Label(line);
          entry.setWrapText(true);
          entry.getStyleClass().add("section-copy");
          logsBox.getChildren().add(entry);
        }
      }

      contentArea.getChildren().setAll(logsBox);
    } catch (AuctionException e) {
      showError("Failed to load logs: " + e.getMessage());
    } catch (Exception e) {
      showError("Failed to load logs: " + e.getMessage());
    }
  }

  @FXML
  private void handleRefreshDashboard() {
    showUsers();
  }

  @FXML
  private void handleLogout() {
    try {
      ClientContext ctx = ClientContext.getInstance();
      ctx.getRmiProvider().getService().logout(ctx.getSessionToken());
      ctx.clearSession();
      ctx.getViewLoader().loadView("login.fxml");
    } catch (Exception e) {
      showError("Logout failed: " + e.getMessage());
    }
  }

  private IAuctionService getService() throws Exception {
    return ClientContext.getInstance().getRmiProvider().getService();
  }

  private Node buildHeader(String title, String subtitle) {
    VBox header = new VBox(4);
    header.getStyleClass().add("panel-card");

    Label titleLabel = new Label(title);
    titleLabel.getStyleClass().add("section-title");

    Label subtitleLabel = new Label(subtitle);
    subtitleLabel.getStyleClass().add("section-copy");
    subtitleLabel.setWrapText(true);

    header.getChildren().addAll(titleLabel, subtitleLabel);
    return header;
  }

  private TableView<User> buildUsersTable(List<User> users) {
    TableView<User> table = new TableView<>();
    table.getStyleClass().add("data-table");

    TableColumn<User, String> usernameCol = new TableColumn<>("Username");
    usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
    usernameCol.setPrefWidth(180);

    TableColumn<User, String> roleCol = new TableColumn<>("Role");
    roleCol.setCellValueFactory(new PropertyValueFactory<>("roleType"));
    roleCol.setPrefWidth(100);

    TableColumn<User, String> createdCol = new TableColumn<>("Created At");
    createdCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
    createdCol.setPrefWidth(220);

    table.getColumns().addAll(usernameCol, roleCol, createdCol);
    table.getItems().setAll(users);
    return table;
  }

  private TableView<AuctionItem> buildAuctionsTable(
    ObservableList<AuctionItem> auctions
  ) {
    TableView<AuctionItem> table = new TableView<>();
    table.getStyleClass().add("auction-table");

    // double-click to open detail
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

    TableColumn<AuctionItem, Integer> idCol = new TableColumn<>("ID");
    idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
    idCol.setPrefWidth(70);

    TableColumn<AuctionItem, String> titleCol = new TableColumn<>("Title");
    titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
    titleCol.setPrefWidth(200);

    TableColumn<AuctionItem, String> sellerCol = new TableColumn<>("Seller");
    sellerCol.setCellValueFactory(new PropertyValueFactory<>("sellerUsername"));
    sellerCol.setPrefWidth(140);

    TableColumn<AuctionItem, String> statusCol = new TableColumn<>("Status");
    statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
    statusCol.setPrefWidth(100);

    TableColumn<AuctionItem, Long> bidCol = new TableColumn<>("Current Bid");
    bidCol.setCellValueFactory(new PropertyValueFactory<>("currentBidCents"));
    bidCol.setPrefWidth(130);
    bidCol.setCellFactory(col ->
      new javafx.scene.control.TableCell<>() {
        @Override
        protected void updateItem(Long value, boolean empty) {
          super.updateItem(value, empty);
          setText(empty || value == null ? null : Constants.formatCents(value));
        }
      }
    );

    // thumbnail column
    TableColumn<AuctionItem, AuctionItem> thumbCol = new TableColumn<>("");
    thumbCol.setCellValueFactory(param ->
      new ReadOnlyObjectWrapper<>(param.getValue())
    );
    thumbCol.setPrefWidth(64);
    thumbCol.setCellFactory(col ->
      new TableCell<>() {
        private final ImageView imageView = new ImageView();

        {
          imageView.setFitWidth(54);
          imageView.setFitHeight(54);
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

    // actions column
    TableColumn<AuctionItem, AuctionItem> actions = new TableColumn<>(
      "Actions"
    );
    actions.setCellValueFactory(param ->
      new ReadOnlyObjectWrapper<>(param.getValue())
    );
    actions.setPrefWidth(140);
    actions.setCellFactory(col ->
      new TableCell<>() {
        private final MenuButton menuButton = new MenuButton("Actions");

        {
          menuButton.getStyleClass().add(Styles.FLAT);
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

    table
      .getColumns()
      .addAll(thumbCol, idCol, titleCol, sellerCol, statusCol, bidCol, actions);
    table.setItems(auctions);
    return table;
  }

  // --- Helpers for thumbnails and actions ---

  private final Map<String, Image> thumbnailCache = new ConcurrentHashMap<>();
  private static final Image PLACEHOLDER = loadPlaceholder();

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
          return bytes == null || bytes.length == 0
            ? null
            : new Image(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
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

  private java.util.List<MenuItem> buildActionMenu(AuctionItem item) {
    MenuItem viewDetails = new MenuItem("View Details");
    viewDetails.setOnAction(e -> openAuctionDetail(item));

    if (ownsListing(item) && isActive(item)) {
      MenuItem cancelListing = new MenuItem("Cancel Listing");
      cancelListing.setOnAction(e ->
        performAuctionAction(item, this::cancelAuction)
      );
      return List.of(viewDetails, cancelListing);
    }

    if (ownsListing(item) && isRelistable(item)) {
      MenuItem relist = new MenuItem("Relist");
      relist.setOnAction(e -> performAuctionAction(item, this::relistAuction));
      return List.of(viewDetails, relist);
    }

    return List.of(viewDetails);
  }

  private boolean ownsListing(AuctionItem item) {
    String username = ClientContext.getInstance().getUsername();
    return (
      item != null &&
      username != null &&
      username.equals(item.getSellerUsername())
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
    if (item == null || item.getStatus() == null) return false;
    String status = item.getStatus().trim().toUpperCase(Locale.ROOT);
    return (
      status.equals(Constants.STATUS_EXPIRED) ||
      status.equals(Constants.STATUS_CANCELLED) ||
      status.equals("ENDED")
    );
  }

  private void performAuctionAction(AuctionItem item, AuctionAction action) {
    if (item == null) return;
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Confirm Action");
    alert.setHeaderText(item.getTitle());
    alert.setContentText("Proceed with this auction action?");
    ButtonType yesButton = new ButtonType("Proceed", ButtonBar.ButtonData.YES);
    ButtonType noButton = new ButtonType("Cancel", ButtonBar.ButtonData.NO);
    alert.getButtonTypes().setAll(yesButton, noButton);
    Optional<ButtonType> result = alert.showAndWait();
    if (result.isPresent() && result.get() == yesButton) {
      try {
        action.execute(item);
        showAuctions();
      } catch (Exception e) {
        showError("Action failed: " + e.getMessage());
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
    if (item == null) return;
    try {
      ClientContext ctx = ClientContext.getInstance();
      ctx.setPreviousViewName("admin_panel.fxml");
      ctx.setCurrentAuctionId(item.getId());
      AuctionDetailController controller = ctx
        .getViewLoader()
        .<AuctionDetailController>loadView("auction_detail.fxml");
      controller.setAuction(item);
    } catch (Exception e) {
      showError("Open detail failed: " + e.getMessage());
    }
  }

  private interface AuctionAction {
    void execute(AuctionItem item) throws Exception;
  }

  private void showError(String message) {
    contentArea.getChildren().setAll(buildHeader("Admin Panel", message));
  }
}
