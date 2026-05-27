package com.auction.client.controllers;

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
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Popup;
import javafx.util.Duration;

public class AuctionDetailController {

  @FXML
  private Label auctionTitleLabel, auctionDescriptionLabel, currentBidLabel, timeLeftLabel, highestBidderLabel, bidStatusLabel;

  @FXML
  private Button placeBidButton;

  @FXML
  private TextField bidAmountField;

  @FXML
  private ProgressIndicator bidSpinner;

  @FXML
  private ImageView heroImageView, thumb1View, thumb2View, thumb3View;

  private PollingService pollingService;
  private int auctionId = -1;
  private AuctionItem currentItem;
  private final Executor executor = Executors.newCachedThreadPool();
  private static final Image PLACEHOLDER = loadPlaceholder();

  public void loadAuction(int id) {
    this.auctionId = id;
    try {
      var service = ClientContext.getInstance().getRmiProvider().getService();
      this.pollingService = new PollingService();
      this.currentItem = service.getAuctionById(id);

      Platform.runLater(() -> updateUi(currentItem));
      loadDetailThumbnail(id, 0, heroImageView);
      loadDetailThumbnail(id, 1, thumb1View);
      loadDetailThumbnail(id, 2, thumb2View);
      loadDetailThumbnail(id, 3, thumb3View);

      pollingService.startPolling(
        () -> {
          try {
            AuctionItem fresh = service.getAuctionById(id);
            if (
              fresh != null &&
              currentItem != null &&
              !fresh.getEndTime().equals(currentItem.getEndTime())
            ) {
              Platform.runLater(() -> showToast("Timer Extended!"));
            }
            this.currentItem = fresh;
            Platform.runLater(() -> updateUi(fresh));
          } catch (Exception ignored) {}
        },
        2
      );
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void updateUi(AuctionItem item) {
    if (item == null) return;
    auctionTitleLabel.setText(
      item.getTitle() == null ? "Auction Detail" : item.getTitle()
    );
    auctionDescriptionLabel.setText(
      item.getDescription() == null
        ? "No description provided"
        : item.getDescription()
    );
    currentBidLabel.setText(Constants.formatCents(item.getCurrentBidCents()));
    highestBidderLabel.setText(
      item.getHighestBidderUsername() == null
        ? "N/A"
        : item.getHighestBidderUsername()
    );
    timeLeftLabel.setText(formatTime(item.getEndTime()));
  }

  private String formatTime(String iso) {
    try {
      var d = java.time.Duration.between(Instant.now(), Instant.parse(iso));
      if (d.isNegative() || d.isZero()) return "Ended";
      return String.format("%02dh %02dm", d.toHours(), d.toMinutesPart());
    } catch (Exception e) {
      return "--:--";
    }
  }

  @FXML
  private void handlePlaceBid() {
    try {
      double amount = Double.parseDouble(bidAmountField.getText().trim());
      long cents = Math.round(amount * 100);
      long expected = currentItem.getCurrentBidCents();

      setBidState(true);
      CompletableFuture.runAsync(
        () -> {
          try {
            var ctx = ClientContext.getInstance();
            ctx
              .getRmiProvider()
              .getService()
              .placeBid(auctionId, cents, expected, ctx.getSessionToken());
            Platform.runLater(() -> {
              bidStatusLabel.setText("Bid submitted");
              bidAmountField.clear();
              animateHero(1.06, 300, 2);
              showToast("Bid placed");
            });
          } catch (Exception e) {
            String msg = resolveBidMessage(e);
            Platform.runLater(() -> {
              bidStatusLabel.setText(msg);
              animateShake();
            });
          } finally {
            Platform.runLater(() -> setBidState(false));
          }
        },
        executor
      );
    } catch (Exception e) {
      bidStatusLabel.setText("Invalid amount");
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
        return "Bid must be higher than current price.";
      }
      if (current instanceof AuctionException) {
        return current.getMessage();
      }
      current = current.getCause();
    }
    return "Failed to place bid";
  }

  private void setBidState(boolean working) {
    placeBidButton.setDisable(working);
    bidAmountField.setDisable(working);
    bidSpinner.setVisible(working);
  }

  private void animateShake() {
    TranslateTransition tt = new TranslateTransition(
      Duration.millis(50),
      bidAmountField
    );
    tt.setByX(10f);
    tt.setCycleCount(6);
    tt.setAutoReverse(true);
    tt.play();
  }

  private void animateHero(double scale, int ms, int cycles) {
    ScaleTransition st = new ScaleTransition(
      Duration.millis(ms),
      heroImageView
    );
    st.setToX(scale);
    st.setToY(scale);
    st.setAutoReverse(true);
    st.setCycleCount(cycles);
    st.play();
  }

  private void showToast(String msg) {
    Label lbl = new Label(msg);
    lbl.setStyle(
      "-fx-background-color: #28a043; -fx-text-fill: white; -fx-padding: 8px; -fx-background-radius: 5px;"
    );
    Popup p = new Popup();
    p.getContent().add(lbl);
    p.show(heroImageView.getScene().getWindow());
    PauseTransition pt = new PauseTransition(Duration.seconds(1.5));
    pt.setOnFinished(e -> p.hide());
    pt.play();
  }

  private void loadDetailThumbnail(int id, int idx, ImageView view) {
    CompletableFuture.supplyAsync(
      () -> {
        try {
          byte[] bytes = ClientContext.getInstance()
            .getRmiProvider()
            .getService()
            .getThumbnail(id, idx);
          return (bytes == null || bytes.length == 0)
            ? null
            : new Image(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
          return null;
        }
      },
      ThumbnailExecutor.getExecutor()
    ).thenAccept(img ->
      Platform.runLater(() -> view.setImage(img != null ? img : PLACEHOLDER))
    );
  }

  @FXML
  private void handleThumb1Click() {
    updateHero(thumb1View.getImage());
  }

  @FXML
  private void handleThumb2Click() {
    updateHero(thumb2View.getImage());
  }

  @FXML
  private void handleThumb3Click() {
    updateHero(thumb3View.getImage());
  }

  private void updateHero(Image img) {
    if (img != null) heroImageView.setImage(img);
  }

  public void showHeroImageIndex(int index) {
    switch (index) {
      case 1:
        updateHero(thumb1View.getImage());
        break;
      case 2:
        updateHero(thumb2View.getImage());
        break;
      case 3:
        updateHero(thumb3View.getImage());
        break;
      default:
        break;
    }
  }

  @FXML
  private void handleBackToGallery() {
    if (pollingService != null) pollingService.shutdown();
    try {
      ClientContext ctx = ClientContext.getInstance();
      ctx
        .getViewLoader()
        .loadView(
          ctx.getPreviousViewName() == null
            ? "gallery.fxml"
            : ctx.getPreviousViewName()
        );
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void shutdown() {
    if (pollingService != null) pollingService.shutdown();
  }

  private static Image loadPlaceholder() {
    InputStream s = AuctionDetailController.class.getResourceAsStream(
      "/images/placeholder.png"
    );
    return (s == null) ? null : new Image(s);
  }
}
