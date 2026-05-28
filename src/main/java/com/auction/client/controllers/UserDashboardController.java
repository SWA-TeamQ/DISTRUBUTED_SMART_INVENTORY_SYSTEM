package com.auction.client.controllers;

import atlantafx.base.theme.Styles;
import com.auction.client.core.ClientContext;
import com.auction.client.service.PollingService;
import com.auction.client.service.ThumbnailExecutor;
import com.auction.client.state.AuctionUiState;
import com.auction.client.util.AuctionCsvExporter;
import com.auction.client.util.MockDataGenerator;
import com.auction.shared.Constants;
import com.auction.shared.interfaces.IAuctionService;
import com.auction.shared.models.AuctionItem;
import com.auction.shared.models.Bid;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.Callback;

public class UserDashboardController {

  private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(
    Locale.US
  );

  static {
    CURRENCY.setCurrency(java.util.Currency.getInstance("ETB"));
  }

  @FXML
  private Label statusLabel;

  @FXML
  private Label marketCountLabel;

  @FXML
  private Label listingsCountLabel;

  @FXML
  private Label bidsCountLabel;

  @FXML
  private Label winsCountLabel;

  @FXML
  private Button createAuctionButton;

  @FXML
  private Button galleryButton;

  @FXML
  private Button exportButton;

  @FXML
  private Button refreshButton;

  @FXML
  private TableView<AuctionItem> marketTable;

  @FXML
  private TableView<AuctionItem> myListingsTable;

  @FXML
  private TableView<Bid> myBidsTable;

  @FXML
  private TableView<AuctionItem> wonAuctionsTable;

  @FXML
  private TableColumn<AuctionItem, AuctionItem> marketThumbnailColumn;

  @FXML
  private TableColumn<AuctionItem, AuctionItem> marketActionsColumn;

  @FXML
  private TableColumn<AuctionItem, Long> marketCurrentBidColumn;

  @FXML
  private TableColumn<AuctionItem, AuctionItem> myListingsThumbnailColumn;

  @FXML
  private TableColumn<AuctionItem, AuctionItem> myListingsActionsColumn;

  @FXML
  private TableColumn<AuctionItem, Long> myListingsCurrentBidColumn;

  @FXML
  private TableColumn<Bid, Long> myBidsAmountColumn;

  @FXML
  private TableColumn<AuctionItem, Long> wonCurrentBidColumn;

  private final AuctionUiState auctionState = AuctionUiState.getInstance();
  private final ObservableList<AuctionItem> myListings =
    FXCollections.observableArrayList();
  private final ObservableList<Bid> myBids =
    FXCollections.observableArrayList();
  private final ObservableList<AuctionItem> wonAuctions =
    FXCollections.observableArrayList();
  private final FilteredList<AuctionItem> myListingsView = new FilteredList<>(
    myListings,
    item -> true
  );
  private final Map<String, Image> thumbnailCache = new ConcurrentHashMap<>();
  private PollingService pollingService;
  private static final Image PLACEHOLDER = loadPlaceholder();

  @FXML
  public void initialize() {
    configureTopActions();
    configureAuctionTable(
      marketTable,
      marketThumbnailColumn,
      marketActionsColumn
    );
    configureAuctionTable(
      myListingsTable,
      myListingsThumbnailColumn,
      myListingsActionsColumn
    );
    configureAuctionTable(wonAuctionsTable, null, null);
    configureMoneyColumns();

    marketTable.setItems(auctionState.getActiveAuctions());
    myListingsTable.setItems(myListingsView);
    myBidsTable.setItems(myBids);
    wonAuctionsTable.setItems(wonAuctions);

    pollingService = new PollingService();
    pollingService.startPolling(
      () -> Platform.runLater(this::refreshDashboard),
      2
    );
    refreshDashboard();
  }

  private void configureTopActions() {
    if (createAuctionButton != null) createAuctionButton
      .getStyleClass()
      .add(Styles.ACCENT);
    if (galleryButton != null) galleryButton.getStyleClass().add(Styles.FLAT);
    if (exportButton != null) exportButton.getStyleClass().add(Styles.FLAT);
    if (refreshButton != null) refreshButton.getStyleClass().add(Styles.FLAT);
  }

  private void configureMoneyColumns() {
    if (marketCurrentBidColumn != null) marketCurrentBidColumn.setCellFactory(
      currencyCellFactory()
    );
    if (
      myListingsCurrentBidColumn != null
    ) myListingsCurrentBidColumn.setCellFactory(currencyCellFactory());
    if (myBidsAmountColumn != null) myBidsAmountColumn.setCellFactory(
      currencyCellFactory()
    );
    if (wonCurrentBidColumn != null) wonCurrentBidColumn.setCellFactory(
      currencyCellFactory()
    );
  }

  private <S> Callback<
    TableColumn<S, Long>,
    TableCell<S, Long>
  > currencyCellFactory() {
    return column ->
      new TableCell<>() {
        @Override
        protected void updateItem(Long value, boolean empty) {
          super.updateItem(value, empty);
          setText(empty || value == null ? null : formatCurrency(value));
        }
      };
  }

  private void configureAuctionTable(
    TableView<AuctionItem> table,
    TableColumn<AuctionItem, AuctionItem> thumbnailColumn,
    TableColumn<AuctionItem, AuctionItem> actionsColumn
  ) {
    if (table == null) {
      return;
    }

    table.setRowFactory(tv -> {
      TableRow<AuctionItem> row = new TableRow<>();
      row.setOnMouseClicked(event -> {
        if (
          event.getButton() == MouseButton.PRIMARY &&
          event.getClickCount() == 2 &&
          !row.isEmpty()
        ) {
          openAuctionDetail(row.getItem(), "user_dashboard.fxml");
        }
      });
      return row;
    });

    if (thumbnailColumn != null) {
      thumbnailColumn.setCellValueFactory(param ->
        new ReadOnlyObjectWrapper<>(param.getValue())
      );
      thumbnailColumn.setCellFactory(column ->
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
    }

    if (actionsColumn != null) {
      actionsColumn.setCellValueFactory(param ->
        new ReadOnlyObjectWrapper<>(param.getValue())
      );
      actionsColumn.setCellFactory(column ->
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
    }
  }

  private List<MenuItem> buildActionMenu(AuctionItem item) {
    MenuItem viewDetails = new MenuItem("View Details");
    viewDetails.setOnAction(e ->
      openAuctionDetail(item, "user_dashboard.fxml")
    );

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

  @FXML
  private void handleCreateAuction() {
    try {
      FXMLLoader loader = new FXMLLoader(
        getClass().getResource("/fxml/create_auction.fxml")
      );
      Parent content = loader.load();
      CreateAuctionDialogController controller = loader.getController();

      Dialog<ButtonType> dialog = new Dialog<>();
      dialog.setTitle("Create Auction");
      Window owner = window();
      if (owner != null) {
        dialog.initOwner(owner);
      }
      dialog.initModality(Modality.WINDOW_MODAL);
      ButtonType createType = new ButtonType(
        "Create Auction",
        ButtonBar.ButtonData.OK_DONE
      );
      dialog
        .getDialogPane()
        .getButtonTypes()
        .addAll(createType, ButtonType.CANCEL);
      dialog.getDialogPane().setContent(content);
      dialog.getDialogPane().getStylesheets().add(styleSheet());
      dialog
        .getDialogPane()
        .getStyleClass()
        .addAll("panel-card", "dialog-surface");

      dialog.setOnShown(event -> {
        Node createNode = dialog.getDialogPane().lookupButton(createType);
        if (createNode != null) {
          createNode.addEventFilter(ActionEvent.ACTION, action -> {
            if (!controller.validateFields()) {
              action.consume();
            }
          });
        }
      });

      Optional<ButtonType> result = dialog.showAndWait();
      if (result.isEmpty() || result.get() != createType) {
        return;
      }

      AuctionItem draft = controller.buildAuctionDraft(
        ClientContext.getInstance().getUsername()
      );
      ClientContext ctx = ClientContext.getInstance();
      int auctionId = ctx
        .getRmiProvider()
        .getService()
        .createAuction(
          draft,
          controller.getImage1Bytes(),
          controller.getImage2Bytes(),
          controller.getImage3Bytes(),
          ctx.getSessionToken()
        );

      statusLabel.setText("Auction #" + auctionId + " created.");
      refreshDashboard();
    } catch (Exception e) {
      statusLabel.setText("Create failed: " + e.getMessage());
    }
  }

  @FXML
  private void handleRefreshDashboard() {
    refreshDashboard();
  }

  @FXML
  private void handleOpenGallery() {
    shutdownPolling();
    try {
      ClientContext.getInstance().getViewLoader().loadView("gallery.fxml");
    } catch (IOException e) {
      statusLabel.setText("Navigation failed: " + e.getMessage());
    }
  }

  @FXML
  private void handleExportCSV() {
    try {
      FileChooser chooser = new FileChooser();
      chooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
      chooser.setInitialFileName("auctions.csv");
      File out = chooser.showSaveDialog(window());
      if (out == null) {
        return;
      }

      AuctionCsvExporter.writeToFile(marketTable.getItems(), out.toPath());
      statusLabel.setText("CSV exported to " + out.getName());
    } catch (Exception e) {
      statusLabel.setText("Export failed: " + e.getMessage());
    }
  }

  @FXML
  private void handleLogout() {
    try {
      shutdownPolling();
      ClientContext ctx = ClientContext.getInstance();
      ctx.getRmiProvider().getService().logout(ctx.getSessionToken());
      ctx.clearSession();
      ctx.getViewLoader().loadView("login.fxml");
    } catch (Exception e) {
      statusLabel.setText("Logout failed: " + e.getMessage());
    }
  }

  private void refreshDashboard() {
    try {
      ClientContext ctx = ClientContext.getInstance();
      IAuctionService service = ctx.getRmiProvider().getService();
      String token = ctx.getSessionToken();
      String username = ctx.getUsername();

      List<AuctionItem> active = sortById(service.getActiveAuctions());
      List<AuctionItem> listings = sortById(
        service.getAuctionsBySeller(username, token)
      );
      List<Bid> bids = service.getMyBids(token);
      List<AuctionItem> won = sortById(service.getMyWonAuctions(token));

      auctionState.getActiveAuctions().setAll(active);
      myListings.setAll(listings);
      myListingsView.setPredicate(item -> item != null && ownsListing(item));
      myBids.setAll(bids == null ? List.of() : bids);
      wonAuctions.setAll(won);

      updateCount(marketCountLabel, auctionState.getActiveAuctions().size());
      updateCount(listingsCountLabel, myListings.size());
      updateCount(bidsCountLabel, myBids.size());
      updateCount(winsCountLabel, wonAuctions.size());

      statusLabel.setText("Dashboard updated.");
    } catch (Exception e) {
      auctionState
        .getActiveAuctions()
        .setAll(MockDataGenerator.getMockAuctions());
      updateCount(marketCountLabel, auctionState.getActiveAuctions().size());
      updateCount(listingsCountLabel, myListings.size());
      updateCount(bidsCountLabel, myBids.size());
      updateCount(winsCountLabel, wonAuctions.size());
      statusLabel.setText("Using mock auctions: " + e.getMessage());
    }
  }

  private void performAuctionAction(AuctionItem item, AuctionAction action) {
    if (item == null) {
      return;
    }

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
        refreshDashboard();
      } catch (Exception e) {
        statusLabel.setText("Action failed: " + e.getMessage());
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
    Instant newEnd = Instant.now().plus(Duration.ofDays(1));
    ctx
      .getRmiProvider()
      .getService()
      .relistAuction(item.getId(), newEnd.toString(), ctx.getSessionToken());
  }

  private void openAuctionDetail(AuctionItem item, String previousViewName) {
    if (item == null) {
      return;
    }

    shutdownPolling();
    try {
      ClientContext ctx = ClientContext.getInstance();
      ctx.setPreviousViewName(previousViewName);
      ctx.setCurrentAuctionId(item.getId());
      AuctionDetailController controller = ctx
        .getViewLoader()
        .loadView("auction_detail.fxml");
      controller.setAuction(item);
    } catch (Exception e) {
      statusLabel.setText("Open detail failed: " + e.getMessage());
    }
  }

  private void updateCount(Label label, int count) {
    if (label != null) {
      label.setText(String.valueOf(count));
    }
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

  private List<AuctionItem> sortById(List<AuctionItem> items) {
    if (items == null) {
      return List.of();
    }
    return items
      .stream()
      .filter(item -> item != null)
      .sorted(Comparator.comparingInt(AuctionItem::getId))
      .toList();
  }

  private String formatCurrency(long cents) {
    return CURRENCY.format(cents / 100.0);
  }

  private Window window() {
    Scene scene =
      createAuctionButton == null ? null : createAuctionButton.getScene();
    if (scene == null && marketTable != null) {
      scene = marketTable.getScene();
    }
    return scene == null ? null : scene.getWindow();
  }

  private String styleSheet() {
    return getClass().getResource("/css/style.css").toExternalForm();
  }

  private void shutdownPolling() {
    if (pollingService != null) {
      pollingService.shutdown();
    }
  }

  private static Image loadPlaceholder() {
    try (
      var stream = UserDashboardController.class.getResourceAsStream(
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

  private interface AuctionAction {
    void execute(AuctionItem item) throws Exception;
  }
}
