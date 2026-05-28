package com.auction.client.controllers;

import atlantafx.base.theme.Styles;
import com.auction.client.core.ClientContext;
import com.auction.client.service.PollingService;
import com.auction.client.service.ThumbnailExecutor;
import com.auction.shared.Constants;
import com.auction.shared.exceptions.AuctionClosedException;
import com.auction.shared.exceptions.AuctionException;
import com.auction.shared.exceptions.InsufficientBidException;
import com.auction.shared.exceptions.SelfBidException;
import com.auction.shared.models.AuctionItem;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public class AuctionDetailController {

  private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(
    Locale.US
  );

  static {
    CURRENCY.setCurrency(java.util.Currency.getInstance("ETB"));
  }

  @FXML
  private Label titleLabel;

  @FXML
  private Label descriptionLabel;

  @FXML
  private Label statusLabel;

  @FXML
  private Label sellerLabel;

  @FXML
  private Label currentHighestBidLabel;

  @FXML
  private Label minimumBidLabel;

  @FXML
  private Label highestBidderLabel;

  @FXML
  private Label bidStatusLabel;

  @FXML
  private Label timeLeftLabel;

  @FXML
  private Button placeBidButton;

  @FXML
  private TextField bidAmountField;

  @FXML
  private ImageView heroImageView;

  @FXML
  private ListView<Image> thumbnailListView;

  private final ObservableList<Image> thumbnails =
    FXCollections.observableArrayList();
  private PollingService pollingService;
  private AuctionItem currentAuction;
  private static final Image PLACEHOLDER = loadPlaceholder();

  @FXML
  public void initialize() {
    if (titleLabel != null) {
      titleLabel.getStyleClass().add(Styles.TITLE_1);
    }
    if (placeBidButton != null) {
      placeBidButton.getStyleClass().add(Styles.ACCENT);
    }

    bidAmountField.setTextFormatter(
      new TextFormatter<>(change ->
        change.getControlNewText().matches("\\d*(?:\\.\\d{0,2})?")
          ? change
          : null
      )
    );

    thumbnailListView.getStyleClass().add("thumbnail-list");

    thumbnailListView.setItems(thumbnails);
    thumbnailListView.setCellFactory(list ->
      new ListCell<>() {
        private final ImageView imageView = new ImageView();

        {
          imageView.setFitWidth(88);
          imageView.setFitHeight(64);
          imageView.setPreserveRatio(true);
          imageView.getStyleClass().add("image-thumb");
        }

        @Override
        protected void updateItem(Image item, boolean empty) {
          super.updateItem(item, empty);
          if (empty || item == null) {
            setGraphic(null);
            return;
          }
          imageView.setImage(item);
          setGraphic(imageView);
        }
      }
    );

    thumbnailListView
      .getSelectionModel()
      .selectedIndexProperty()
      .addListener((obs, oldValue, newValue) -> {
        if (
          newValue == null ||
          newValue.intValue() < 0 ||
          newValue.intValue() >= thumbnails.size()
        ) {
          return;
        }
        heroImageView.setImage(thumbnails.get(newValue.intValue()));
      });

    bidStatusLabel.setText("Ready to bid.");
    setBidControlsEnabled(false);
  }

  public void setAuction(AuctionItem auction) {
    if (auction == null) {
      return;
    }
    shutdownPolling();
    currentAuction = auction;
    applyAuction(auction);
    loadThumbnails(auction.getId());
    startPolling(auction.getId());
  }

  @FXML
  private void handlePlaceBid() {
    if (currentAuction == null) {
      bidStatusLabel.setText("No auction is selected.");
      return;
    }

    try {
      double amount = Double.parseDouble(bidAmountField.getText().trim());
      long bidCents = Math.round(amount * 100.0);
      long minimumCents = minimumAcceptedBid(currentAuction);

      if (bidCents < minimumCents) {
        bidStatusLabel.setText(
          "Bid must be at least " + formatCurrency(minimumCents) + "."
        );
        shakeBidField();
        return;
      }

      setBidControlsEnabled(true);
      bidStatusLabel.setText("Submitting bid...");

      CompletableFuture.runAsync(
        () -> {
          try {
            ClientContext ctx = ClientContext.getInstance();
            ctx
              .getRmiProvider()
              .getService()
              .placeBid(
                currentAuction.getId(),
                bidCents,
                currentAuction.getCurrentBidCents(),
                ctx.getSessionToken()
              );
            Platform.runLater(() -> {
              bidAmountField.clear();
              bidStatusLabel.setText("Bid submitted successfully.");
              refreshAuction();
            });
          } catch (Exception e) {
            Platform.runLater(() -> {
              bidStatusLabel.setText(resolveBidMessage(e));
              shakeBidField();
            });
          } finally {
            Platform.runLater(() -> setBidControlsEnabled(false));
          }
        },
        ThumbnailExecutor.getExecutor()
      );
    } catch (Exception e) {
      bidStatusLabel.setText("Enter a numeric bid amount.");
      shakeBidField();
    }
  }

  @FXML
  private void handleBackToPreviousView() {
    shutdownPolling();
    try {
      ClientContext ctx = ClientContext.getInstance();
      String previous = ctx.getPreviousViewName();
      ctx
        .getViewLoader()
        .loadView(previous == null ? "user_dashboard.fxml" : previous);
    } catch (Exception e) {
      bidStatusLabel.setText("Navigation failed: " + e.getMessage());
    }
  }

  public void shutdown() {
    shutdownPolling();
  }

  private void startPolling(int auctionId) {
    pollingService = new PollingService();
    pollingService.startPolling(
      () -> {
        try {
          AuctionItem fresh = ClientContext.getInstance()
            .getRmiProvider()
            .getService()
            .getAuctionById(auctionId);
          if (fresh != null) {
            Platform.runLater(() -> {
              currentAuction = fresh;
              applyAuction(fresh);
            });
          }
        } catch (Exception ignored) {}
      },
      2
    );
  }

  private void refreshAuction() {
    if (currentAuction == null) {
      return;
    }
    try {
      AuctionItem fresh = ClientContext.getInstance()
        .getRmiProvider()
        .getService()
        .getAuctionById(currentAuction.getId());
      if (fresh != null) {
        currentAuction = fresh;
        applyAuction(fresh);
      }
    } catch (Exception e) {
      bidStatusLabel.setText("Refresh failed: " + e.getMessage());
    }
  }

  private void applyAuction(AuctionItem auction) {
    if (auction == null) {
      return;
    }

    titleLabel.setText(
      auction.getTitle() == null ? "Auction Detail" : auction.getTitle()
    );
    descriptionLabel.setText(
      auction.getDescription() == null
        ? "No description provided."
        : auction.getDescription()
    );
    sellerLabel.setText(
      auction.getSellerUsername() == null
        ? "Seller: unknown"
        : "Seller: " + auction.getSellerUsername()
    );
    statusLabel.setText(
      auction.getStatus() == null ? "STATUS" : auction.getStatus()
    );
    statusLabel
      .getStyleClass()
      .setAll("status-chip", statusStyle(auction.getStatus()));

    currentHighestBidLabel.setText(
      formatCurrency(auction.getCurrentBidCents())
    );
    highestBidderLabel.setText(
      auction.getHighestBidderUsername() == null
        ? "Highest bidder: No bids yet"
        : "Highest bidder: " + auction.getHighestBidderUsername()
    );
    minimumBidLabel.setText(
      "Minimum next bid: " + formatCurrency(minimumAcceptedBid(auction))
    );
    timeLeftLabel.setText(formatTimeLeft(auction.getEndTime()));

    boolean active =
      auction.getStatus() != null &&
      Constants.STATUS_ACTIVE.equalsIgnoreCase(auction.getStatus());
    setBidControlsEnabled(!active);
    bidAmountField.setDisable(!active);
    placeBidButton.setDisable(!active);
  }

  private void loadThumbnails(int auctionId) {
    thumbnails.clear();
    heroImageView.setImage(PLACEHOLDER);

    for (int index = 0; index < 3; index++) {
      final int thumbnailIndex = index;
      CompletableFuture.supplyAsync(
        () -> {
          try {
            byte[] bytes = ClientContext.getInstance()
              .getRmiProvider()
              .getService()
              .getThumbnail(auctionId, thumbnailIndex);
            return bytes == null || bytes.length == 0
              ? PLACEHOLDER
              : new Image(new ByteArrayInputStream(bytes));
          } catch (Exception e) {
            return PLACEHOLDER;
          }
        },
        ThumbnailExecutor.getExecutor()
      ).thenAccept(image ->
        Platform.runLater(() -> {
          thumbnails.add(image == null ? PLACEHOLDER : image);
          if (thumbnails.size() == 1) {
            thumbnailListView.getSelectionModel().selectFirst();
            heroImageView.setImage(thumbnails.get(0));
          }
        })
      );
    }
  }

  private void setBidControlsEnabled(boolean working) {
    if (placeBidButton != null) {
      placeBidButton.setDisable(
        working || currentAuction == null || !isActive(currentAuction)
      );
    }
    if (bidAmountField != null) {
      bidAmountField.setDisable(
        working || currentAuction == null || !isActive(currentAuction)
      );
    }
  }

  private boolean isActive(AuctionItem item) {
    return (
      item != null &&
      item.getStatus() != null &&
      Constants.STATUS_ACTIVE.equalsIgnoreCase(item.getStatus())
    );
  }

  private long minimumAcceptedBid(AuctionItem auction) {
    if (auction.getHighestBidderUsername() == null) {
      return auction.getStartingPriceCents();
    }
    long increment = Math.max(
      1L,
      Math.round(
        auction.getCurrentBidCents() * Constants.MIN_BID_INCREMENT_PERCENT
      )
    );
    return auction.getCurrentBidCents() + increment;
  }

  private String formatCurrency(long cents) {
    return CURRENCY.format(cents / 100.0);
  }

  private String formatTimeLeft(String iso) {
    try {
      java.time.Duration duration = java.time.Duration.between(
        java.time.Instant.now(),
        java.time.Instant.parse(iso)
      );
      if (duration.isNegative() || duration.isZero()) {
        return "Ended";
      }
      return String.format(
        "%02dh %02dm",
        duration.toHours(),
        duration.toMinutesPart()
      );
    } catch (Exception e) {
      return "--:--";
    }
  }

  private String resolveBidMessage(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if (current instanceof SelfBidException) {
        return "You cannot bid on your own auction.";
      }
      if (current instanceof AuctionClosedException) {
        return "Bidding is closed for this item.";
      }
      if (current instanceof InsufficientBidException) {
        return "Bid must meet the current minimum.";
      }
      if (current instanceof AuctionException) {
        return current.getMessage();
      }
      current = current.getCause();
    }
    return "Failed to place bid.";
  }

  private void shakeBidField() {
    PauseTransition pause = new PauseTransition(Duration.millis(120));
    pause.setOnFinished(event ->
      bidAmountField.setStyle("-fx-border-color: #f85149;")
    );
    pause.play();
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
      InputStream stream = AuctionDetailController.class.getResourceAsStream(
        "/images/placeholder.png"
      )
    ) {
      return stream == null ? null : new Image(stream);
    } catch (IOException e) {
      return null;
    }
  }
}
