package com.auction.client.controllers;

import com.auction.client.core.ClientContext;
import com.auction.client.service.PollingService;
import com.auction.client.service.ThumbnailExecutor;
import com.auction.shared.Constants;
import com.auction.shared.interfaces.IAuctionService;
import com.auction.shared.models.AuctionItem;
import com.auction.shared.models.Bid;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

public class UserDashboardController {

  @FXML
  private TableView<AuctionItem> marketTable, myListingsTable, wonAuctionsTable;

  @FXML
  private TableView<Bid> myBidsTable;

  @FXML
  private TextField titleField, categoryField, priceField, endTimeField;

  @FXML
  private TextArea descArea;

  @FXML
  private Label imagesLabel, statusLabel, marketCountLabel, listingsCountLabel, bidsCountLabel, winsCountLabel;

  private byte[] img1Bytes, img2Bytes, img3Bytes;
  private final Map<String, Image> thumbnailCache = new ConcurrentHashMap<>();
  private static final Image PLACEHOLDER = new Image(
    UserDashboardController.class.getResourceAsStream("/images/placeholder.png")
  );
  private PollingService pollingService;

  @FXML
  public void initialize() {
    pollingService = new PollingService();
    pollingService.startPolling(
      () -> Platform.runLater(this::refreshDashboard),
      2
    );

    setupTableThumbnails(marketTable);
    formatCurrencyColumns();
    setupRowDoubleClick(marketTable);
    setupRowDoubleClick(myListingsTable);
    setupRowDoubleClick(wonAuctionsTable);

    refreshDashboard();
  }

  private void setupTableThumbnails(TableView<AuctionItem> table) {
    TableColumn<AuctionItem, ImageView> thumbCol = new TableColumn<>("");
    thumbCol.setPrefWidth(80);
    thumbCol.setCellFactory(col ->
      new TableCell<>() {
        private final ImageView iv = new ImageView();

        {
          iv.setFitWidth(70);
          iv.setFitHeight(50);
          iv.setPreserveRatio(true);
        }

        @Override
        protected void updateItem(ImageView item, boolean empty) {
          super.updateItem(item, empty);
          if (empty) {
            setGraphic(null);
          } else {
            AuctionItem auction = getTableView().getItems().get(getIndex());
            loadThumbnailAsync(auction.getId(), 0, iv);
            setGraphic(iv);
          }
        }
      }
    );
    table.getColumns().add(0, thumbCol);
  }

  private void setupRowDoubleClick(TableView<AuctionItem> table) {
    table.setRowFactory(tv -> {
      TableRow<AuctionItem> row = new TableRow<>();
      row.setOnMouseClicked(event -> {
        if (!row.isEmpty() && event.getClickCount() == 2) {
          handleOpenAuctionDetail();
        }
      });
      return row;
    });
  }

  private void refreshDashboard() {
    try {
      ClientContext ctx = ClientContext.getInstance();
      IAuctionService service = ctx.getRmiProvider().getService();

      List<AuctionItem> active = service.getActiveAuctions();
      List<AuctionItem> mine = service.getActiveAuctionsBySeller(
        ctx.getUsername(),
        ctx.getSessionToken()
      );
      List<Bid> bids = service.getMyBids(ctx.getSessionToken());
      List<AuctionItem> won = service.getMyWonAuctions(ctx.getSessionToken());

      marketTable.getItems().setAll(active);
      myListingsTable.getItems().setAll(mine);
      myBidsTable.getItems().setAll(bids);
      wonAuctionsTable.getItems().setAll(won);

      updateCount(marketCountLabel, active.size());
      updateCount(listingsCountLabel, mine.size());
      updateCount(bidsCountLabel, bids.size());
      updateCount(winsCountLabel, won.size());

      statusLabel.setText("Dashboard updated.");
    } catch (Exception e) {
      statusLabel.setText("Update failed: " + e.getMessage());
    }
  }

  @FXML
  private void handleRefreshDashboard() {
    refreshDashboard();
  }

  private void updateCount(Label label, int count) {
    if (label != null) label.setText(String.valueOf(count));
  }

  @SuppressWarnings("unchecked")
  private void formatCurrencyColumns() {
    formatCurrencyColumn(marketTable, 5);
    formatCurrencyColumn(myListingsTable, 3);
    formatCurrencyColumn(myBidsTable, 3);
    formatCurrencyColumn(wonAuctionsTable, 3);
  }

  @SuppressWarnings("unchecked")
  private <S> void formatCurrencyColumn(TableView<S> table, int columnIndex) {
    if (table == null || table.getColumns().size() <= columnIndex) return;

    TableColumn<S, Long> column = (TableColumn<S, Long>) table
      .getColumns()
      .get(columnIndex);
    column.setCellFactory(col ->
      new TableCell<S, Long>() {
        @Override
        protected void updateItem(Long value, boolean empty) {
          super.updateItem(value, empty);
          setText(empty || value == null ? null : Constants.formatCents(value));
        }
      });
  }

  @FXML
  private void handleOpenAuctionDetail() {
    AuctionItem selected = getSelectedAuction();
    if (selected != null) {
      try {
        if (pollingService != null) pollingService.shutdown();
        ClientContext ctx = ClientContext.getInstance();
        ctx.setCurrentAuctionId(selected.getId());
        ctx.setPreviousViewName("user_dashboard.fxml");
        ctx.getViewLoader().loadView("auction_detail.fxml");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private AuctionItem getSelectedAuction() {
    if (
      marketTable.getSelectionModel().getSelectedItem() != null
    ) return marketTable.getSelectionModel().getSelectedItem();
    if (
      myListingsTable.getSelectionModel().getSelectedItem() != null
    ) return myListingsTable.getSelectionModel().getSelectedItem();
    if (
      wonAuctionsTable.getSelectionModel().getSelectedItem() != null
    ) return wonAuctionsTable.getSelectionModel().getSelectedItem();
    return null;
  }

  private void loadThumbnailAsync(int auctionId, int index, ImageView target) {
    String key = auctionId + ":" + index;
    if (thumbnailCache.containsKey(key)) {
      target.setImage(thumbnailCache.get(key));
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
        } catch (Exception e) {
          return null;
        }
      },
      ThumbnailExecutor.getExecutor()
    ).thenAccept(image -> {
      Image finalImg = (image != null) ? image : PLACEHOLDER;
      thumbnailCache.put(key, finalImg);
      Platform.runLater(() -> target.setImage(finalImg));
    });
  }

  @FXML
  private void handleCreateAuction() {
    try {
      long cents = (long) (Double.parseDouble(priceField.getText()) * 100);
      Instant end = Instant.now().plus(
        Duration.ofMinutes(Integer.parseInt(endTimeField.getText()))
      );

      AuctionItem item = new AuctionItem(
        0,
        titleField.getText(),
        descArea.getText(),
        categoryField.getText(),
        cents,
        ClientContext.getInstance().getUsername(),
        Instant.now().toString(),
        end.toString(),
        null
      );

      ClientContext ctx = ClientContext.getInstance();
      int id = ctx
        .getRmiProvider()
        .getService()
        .createAuction(
          item,
          img1Bytes,
          img2Bytes,
          img3Bytes,
          ctx.getSessionToken()
        );

      statusLabel.setText("Auction #" + id + " created.");
      clearAuctionForm();
      refreshDashboard();
    } catch (Exception e) {
      statusLabel.setText("Error: " + e.getMessage());
    }
  }

  private void clearAuctionForm() {
    titleField.clear();
    descArea.clear();
    categoryField.clear();
    priceField.clear();
    endTimeField.clear();
    img1Bytes = img2Bytes = img3Bytes = null;
    imagesLabel.setText("No images selected");
  }

  @FXML
  private void handleCancelAuction() {
    performActionWithConfirmation(
      "Cancel Auction",
      "Cancel the selected auction?",
      "This will permanently cancel the listing.",
      myListingsTable.getSelectionModel().getSelectedItem(),
      s ->
        ClientContext.getInstance()
          .getRmiProvider()
          .getService()
          .cancelAuction(
            s.getId(),
            ClientContext.getInstance().getSessionToken()
          )
    );
  }

  @FXML
  private void handleRelistAuction() {
    performActionWithConfirmation(
      "Relist Auction",
      "Relist the selected auction?",
      "A new auction record will be created with a fresh end time.",
      myListingsTable.getSelectionModel().getSelectedItem(),
      s ->
        ClientContext.getInstance()
          .getRmiProvider()
          .getService()
          .relistAuction(
            s.getId(),
            Instant.now().plus(Duration.ofDays(1)).toString(),
            ClientContext.getInstance().getSessionToken()
          )
    );
  }

  private interface AuctionAction {
    void execute(AuctionItem item) throws Exception;
  }

  private void performAction(AuctionItem item, AuctionAction action) {
    if (item == null) return;
    try {
      action.execute(item);
      refreshDashboard();
    } catch (Exception e) {
      statusLabel.setText("Action failed: " + e.getMessage());
    }
  }

  private void performActionWithConfirmation(
    String title,
    String header,
    String content,
    AuctionItem item,
    AuctionAction action
  ) {
    if (item == null) {
      statusLabel.setText("Select an auction first.");
      return;
    }

    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle(title);
    alert.setHeaderText(header);
    alert.setContentText(content);
    ButtonType yesButton = new ButtonType("YES", ButtonBar.ButtonData.YES);
    ButtonType noButton = new ButtonType("NO", ButtonBar.ButtonData.NO);
    alert.getButtonTypes().setAll(yesButton, noButton);
    Optional<ButtonType> result = alert.showAndWait();
    if (result.isPresent() && result.get() == yesButton) {
      performAction(item, action);
    }
  }

  @FXML
  private void handleLogout() {
    try {
      ClientContext ctx = ClientContext.getInstance();
      ctx.getRmiProvider().getService().logout(ctx.getSessionToken());
      ctx.clearSession();
      ctx.getViewLoader().loadView("login.fxml");
    } catch (Exception e) {
      statusLabel.setText("Logout failed");
    }
  }

  @FXML
  private void handleOpenGallery() {
    if (pollingService != null) pollingService.shutdown();
    try {
      ClientContext.getInstance().getViewLoader().loadView("gallery.fxml");
    } catch (Exception e) {
      statusLabel.setText("Navigation failed: " + e.getMessage());
    }
  }

  @FXML
  private void handleExportCSV() {
    try {
      ClientContext ctx = ClientContext.getInstance();
      byte[] bytes = ctx
        .getRmiProvider()
        .getService()
        .exportAuctionsToCSV(ctx.getSessionToken());

      FileChooser fc = new FileChooser();
      fc
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
      fc.setInitialFileName("auctions.csv");
      File out = fc.showSaveDialog(marketTable.getScene().getWindow());
      if (out != null) {
        Files.write(out.toPath(), bytes);
        statusLabel.setText("CSV exported to " + out.getName());
      }
    } catch (Exception e) {
      statusLabel.setText("Export failed: " + e.getMessage());
    }
  }

  @FXML
  private void handlePickImg1() {
    img1Bytes = pickImage();
    updateImagesLabel();
  }

  @FXML
  private void handlePickImg2() {
    img2Bytes = pickImage();
    updateImagesLabel();
  }

  @FXML
  private void handlePickImg3() {
    img3Bytes = pickImage();
    updateImagesLabel();
  }

  private byte[] pickImage() {
    FileChooser fc = new FileChooser();
    fc
      .getExtensionFilters()
      .add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png"));
    File f = fc.showOpenDialog(marketTable.getScene().getWindow());
    if (f != null && f.length() <= Constants.MAX_IMAGE_SIZE_BYTES) {
      try {
        return Files.readAllBytes(f.toPath());
      } catch (IOException e) {
        return null;
      }
    }
    return null;
  }

  private void updateImagesLabel() {
    int count =
      (img1Bytes != null ? 1 : 0) +
      (img2Bytes != null ? 1 : 0) +
      (img3Bytes != null ? 1 : 0);
    imagesLabel.setText(count + " images selected");
  }
}
