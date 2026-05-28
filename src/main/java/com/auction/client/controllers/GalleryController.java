package com.auction.client.controllers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.auction.client.core.ClientContext;
import com.auction.client.service.PollingService;
import com.auction.client.service.ThumbnailExecutor;
import com.auction.client.state.AuctionUiState;
import com.auction.shared.models.AuctionItem;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

public class GalleryController {

  private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(
    Locale.US
  );

  static {
    CURRENCY.setCurrency(java.util.Currency.getInstance("ETB"));
  }

  @FXML
  private TilePane auctionGrid;

  @FXML
  private TextField searchField;

  @FXML
  private Label auctionCountLabel;

  private final AuctionUiState auctionState = AuctionUiState.getInstance();
  private final Map<String, Image> cache = new ConcurrentHashMap<>();
  private PollingService pollingService;
  private String query = "";
  private static final Image PLACEHOLDER = loadPlaceholder();

  @FXML
  public void initialize() {
    auctionState
      .getActiveAuctions()
      .addListener((ListChangeListener<AuctionItem>) change -> render());
    if (searchField != null) {
      searchField
        .textProperty()
        .addListener((obs, oldValue, newValue) -> {
          query =
            newValue == null ? "" : newValue.trim().toLowerCase(Locale.ROOT);
          render();
        });
    }

    pollingService = new PollingService();
    pollingService.startPolling(
      () -> Platform.runLater(this::refreshAuctions),
      2
    );
    refreshAuctions();
  }

  @FXML
  private void handleSearch() {
    query =
      searchField == null || searchField.getText() == null
        ? ""
        : searchField.getText().trim().toLowerCase(Locale.ROOT);
    render();
  }

  @FXML
  private void handleBackToDashboard() {
    shutdownPolling();
    try {
      ClientContext.getInstance()
        .getViewLoader()
        .loadView("user_dashboard.fxml");
    } catch (Exception e) {
      if (auctionCountLabel != null) {
        auctionCountLabel.setText("Navigation failed: " + e.getMessage());
      }
    }
  }

  private void refreshAuctions() {
    try {
      List<AuctionItem> items = ClientContext.getInstance()
        .getRmiProvider()
        .getService()
        .getActiveAuctions();
      auctionState
        .getActiveAuctions()
        .setAll(items == null ? List.of() : items);
      render();
    } catch (Exception e) {
      render();
    }
  }

  private void render() {
    if (auctionGrid == null) {
      return;
    }

    auctionGrid.getChildren().clear();
    List<AuctionItem> items = auctionState
      .getActiveAuctions()
      .stream()
      .filter(item -> item != null && matchesQuery(item))
      .sorted((left, right) -> Integer.compare(left.getId(), right.getId()))
      .toList();

    if (items.isEmpty()) {
      VBox empty = new VBox(8);
      empty.getStyleClass().add("metric-card");
      empty
        .getChildren()
        .addAll(
          new Label("No auctions found"),
          new Label("Adjust the search filter or wait for the next refresh.")
        );
      auctionGrid.getChildren().add(empty);
      auctionCountLabel.setText("0 auction(s)");
      return;
    }

    for (AuctionItem item : items) {
      auctionGrid.getChildren().add(createCard(item));
    }
    auctionCountLabel.setText(items.size() + " auction(s)");
  }

  private boolean matchesQuery(AuctionItem item) {
    if (query == null || query.isBlank()) {
      return true;
    }
    return (
      contains(item.getTitle(), query) ||
      contains(item.getCategory(), query) ||
      contains(item.getDescription(), query)
    );
  }

  private boolean contains(String value, String needle) {
    return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
  }

  private VBox createCard(AuctionItem item) {
    VBox card = new VBox(12);
    card.getStyleClass().addAll("metric-card", Styles.ELEVATED_1);
    card.setPrefWidth(244);
    card.setMinWidth(244);
    card.setMaxWidth(244);

    ImageView hero = createImageView(220, 150);
    loadThumbAsync(item.getId(), 0, hero);

    HBox rail = new HBox(6);
    for (int index = 0; index < 3; index++) {
      ImageView thumb = createImageView(64, 48);
      loadThumbAsync(item.getId(), index, thumb);
      rail.getChildren().add(thumb);
    }

    Label title = new Label(item.getTitle());
    title.setWrapText(true);
    title.getStyleClass().add("section-title");
    title.setMaxWidth(220);
    title.setPrefWidth(220);

    Label category = new Label(
      item.getCategory() == null ? "" : item.getCategory()
    );
    category.getStyleClass().add("section-copy");

    Label status = new Label(item.getStatus() == null ? "" : item.getStatus());
    status.getStyleClass().addAll("status-chip", statusStyle(item.getStatus()));

    Label bid = new Label(formatCurrency(item.getCurrentBidCents()));
    bid.getStyleClass().add("metric-value");
    bid.setStyle("-fx-font-size: 18px; -fx-font-weight: 700;");

    Label seller = new Label(
      item.getSellerUsername() == null ? "" : "by " + item.getSellerUsername()
    );
    seller.getStyleClass().add("section-copy");

    HBox buttonBox = new HBox(8);
    buttonBox.setAlignment(Pos.CENTER_LEFT);

    Button openButton = new Button("View");
    openButton.getStyleClass().addAll(Styles.FLAT, "secondary-button");
    openButton.setMaxWidth(Double.MAX_VALUE);
    openButton.setOnAction(e -> openDetail(item));

    buttonBox.getChildren().add(openButton);

    card.setOnMouseClicked(event -> {
      if (
        event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1
      ) {
        openDetail(item);
      }
    });

    card
      .getChildren()
      .addAll(hero, rail, title, category, status, bid, seller, buttonBox);
    return card;
  }

  private ImageView createImageView(double width, double height) {
    ImageView imageView = new ImageView();
    imageView.setFitWidth(width);
    imageView.setFitHeight(height);
    imageView.setPreserveRatio(true);
    imageView.getStyleClass().add("image-placeholder");
    return imageView;
  }

  private void openDetail(AuctionItem item) {
    shutdownPolling();
    try {
      ClientContext ctx = ClientContext.getInstance();
      ctx.setPreviousViewName("gallery.fxml");
      ctx.setCurrentAuctionId(item.getId());
      AuctionDetailController controller = ctx
        .getViewLoader()
        .loadView("auction_detail.fxml");
      controller.setAuction(item);
    } catch (Exception e) {
      if (auctionCountLabel != null) {
        auctionCountLabel.setText("Open failed: " + e.getMessage());
      }
    }
  }

  private void loadThumbAsync(int auctionId, int index, ImageView target) {
    String key = auctionId + ":" + index;
    Image cached = cache.get(key);
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
      cache.put(key, finalImage);
      Platform.runLater(() -> target.setImage(finalImage));
    });
  }

  private String formatCurrency(long cents) {
    return CURRENCY.format(cents / 100.0);
  }

  private String statusStyle(String status) {
    if (status == null) {
      return "status-chip-success";
    }
    String normalized = status.trim().toUpperCase(Locale.ROOT);
    return switch (normalized) {
      case "ACTIVE" -> "status-chip-success";
      case "CANCELLED" -> "status-chip-warning";
      case "ENDED", "EXPIRED", "SOLD" -> "status-chip-accent";
      default -> "status-chip-success";
    };
  }

  private void shutdownPolling() {
    if (pollingService != null) {
      pollingService.shutdown();
    }
  }

  private static Image loadPlaceholder() {
    try (
      InputStream stream = GalleryController.class.getResourceAsStream(
        "/images/placeholder.png"
      )
    ) {
      return stream == null ? null : new Image(stream);
    } catch (IOException e) {
      return null;
    }
  }
}
