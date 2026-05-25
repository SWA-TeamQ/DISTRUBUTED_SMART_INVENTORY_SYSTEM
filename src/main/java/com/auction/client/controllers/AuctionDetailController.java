package com.auction.client.controllers;

import com.auction.client.core.ClientContext;
import com.auction.client.service.PollingService;
import com.auction.shared.models.AuctionItem;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;
import javafx.stage.Popup;
import com.auction.client.service.ThumbnailExecutor;

import java.io.IOException;
import java.io.InputStream;

public class AuctionDetailController {

    @FXML private Label auctionTitleLabel;
    @FXML private Label auctionDescriptionLabel;
    @FXML private Label currentBidLabel;
    @FXML private Label timeLeftLabel;
    @FXML private Label highestBidderLabel;
    @FXML private Button placeBidButton;

    private PollingService pollingService;
    private int currentAuctionId = -1;
    private com.auction.shared.models.AuctionItem currentItem;
    @FXML private TextField bidAmountField;
    @FXML private Label bidStatusLabel;
    @FXML private ProgressIndicator bidSpinner;
    @FXML private javafx.scene.image.ImageView heroImageView;
    @FXML private javafx.scene.image.ImageView thumb1View;
    @FXML private javafx.scene.image.ImageView thumb2View;
    @FXML private javafx.scene.image.ImageView thumb3View;
    private final java.util.concurrent.Executor executor = java.util.concurrent.Executors.newCachedThreadPool();
    private static final javafx.scene.image.Image PLACEHOLDER_IMAGE = loadPlaceholderImage();

    @FXML
    public void initialize() {
        // nothing; detail view will be initialized via loadAuction(int)
    }

    public void loadAuction(int auctionId) {
        this.currentAuctionId = auctionId;
        try {
            var service = ClientContext.getInstance().getRmiProvider().getService();
            this.pollingService = new PollingService(service);
            // load initial item state
            AuctionItem initial = service.getAuctionById(auctionId);
            this.currentItem = initial;
            Platform.runLater(() -> updateUi(initial));
            // load thumbnails for detail view
            loadDetailThumbnail(auctionId, 0, heroImageView);
            loadDetailThumbnail(auctionId, 1, thumb1View);
            loadDetailThumbnail(auctionId, 2, thumb2View);
            loadDetailThumbnail(auctionId, 3, thumb3View);
            pollingService.startPolling(auctionId, item -> {
                this.currentItem = item;
                Platform.runLater(() -> updateUi(item));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUi(AuctionItem item) {
        if (item == null) return;
        if (auctionTitleLabel != null) {
            auctionTitleLabel.setText(item.getTitle() == null || item.getTitle().isBlank() ? "Auction Detail" : item.getTitle());
        }
        if (auctionDescriptionLabel != null) {
            String desc = (item.getDescription() == null || item.getDescription().isBlank())
                ? "No description provided"
                : item.getDescription();
            auctionDescriptionLabel.setText(desc);
        }
        currentBidLabel.setText(com.auction.shared.Constants.formatCents(item.getCurrentBidCents()));
        highestBidderLabel.setText(item.getHighestBidderUsername() == null ? "N/A" : item.getHighestBidderUsername());
        if (timeLeftLabel != null) {
            timeLeftLabel.setText(formatTimeLeft(item.getEndTime()));
        }
    }

    private String formatTimeLeft(String endTimeIso) {
        if (endTimeIso == null || endTimeIso.isBlank()) return "--:--";
        try {
            java.time.Instant end = java.time.Instant.parse(endTimeIso);
            java.time.Duration d = java.time.Duration.between(java.time.Instant.now(), end);
            if (d.isNegative() || d.isZero()) return "Ended";
            long hours = d.toHours();
            long minutes = d.minusHours(hours).toMinutes();
            long seconds = d.minusHours(hours).minusMinutes(minutes).toSeconds();
            if (hours > 0) {
                return String.format("%02dh %02dm", hours, minutes);
            }
            return String.format("%02dm %02ds", minutes, seconds);
        } catch (Exception ignored) {
            return "--:--";
        }
    }

    public void shutdown() {
        if (pollingService != null) pollingService.shutdown();
    }

    @FXML
    private void handlePlaceBid() {
        if (currentAuctionId < 0) return;
        String input = bidAmountField.getText();
        if (input == null || input.trim().isEmpty()) {
            bidStatusLabel.setText("Enter bid amount");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(input.trim());
        } catch (NumberFormatException nfe) {
            bidStatusLabel.setText("Invalid amount format");
            return;
        }

        long amountCents = Math.round(amount * 100);
        long expected = currentItem == null ? 0L : currentItem.getCurrentBidCents();

        // optimistic UI update
        long prevBid = currentItem == null ? 0L : currentItem.getCurrentBidCents();
        String prevHighest = currentItem == null ? null : currentItem.getHighestBidderUsername();

        var context = ClientContext.getInstance();
        String you = context.getUsername() == null ? "You" : context.getUsername();

        // apply optimistic change
        if (currentItem != null) {
            currentItem.setCurrentBidCents(amountCents);
            currentItem.setHighestBidderUsername(you);
        }
        Platform.runLater(() -> {
            updateUi(currentItem);
            placeBidButton.setDisable(true);
            bidAmountField.setDisable(true);
            bidSpinner.setVisible(true);
            bidStatusLabel.setText("Submitting...");
        });

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                var service = context.getRmiProvider().getService();
                service.placeBid(currentAuctionId, amountCents, expected, context.getSessionToken());
                Platform.runLater(() -> {
                    bidStatusLabel.setText("Bid submitted");
                    bidAmountField.clear();
                    // show success toast and highlight hero image
                    showToast("Bid placed");
                    animateSuccess();
                });
            } catch (Exception e) {
                // parse server-side AuctionException if present
                String userMsg = "Failed to place bid";
                Throwable cause = e;
                while (cause != null) {
                    if (cause instanceof com.auction.shared.exceptions.AuctionException) {
                        userMsg = cause.getMessage();
                        break;
                    }
                    cause = cause.getCause();
                }
                final String finalMsg = userMsg;
                // rollback optimistic update
                if (currentItem != null) {
                    currentItem.setCurrentBidCents(prevBid);
                    currentItem.setHighestBidderUsername(prevHighest);
                }
                Platform.runLater(() -> {
                    updateUi(currentItem);
                    bidStatusLabel.setText("Failed: " + finalMsg);
                });
            } finally {
                Platform.runLater(() -> {
                    placeBidButton.setDisable(false);
                    bidAmountField.setDisable(false);
                    bidSpinner.setVisible(false);
                });
            }
        }, executor);
    }

    private void loadDetailThumbnail(int auctionId, int index, javafx.scene.image.ImageView target) {
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                var service = ClientContext.getInstance().getRmiProvider().getService();
                byte[] bytes = service.getThumbnail(auctionId, index);
                if (bytes == null || bytes.length == 0) return null;
                return new javafx.scene.image.Image(new java.io.ByteArrayInputStream(bytes));
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }, ThumbnailExecutor.getExecutor()).thenAccept(image -> {
            if (image != null) {
                Platform.runLater(() -> {
                    target.setImage(image);
                    target.setStyle(null);
                });
            } else {
                Platform.runLater(() -> {
                    target.setImage(PLACEHOLDER_IMAGE);
                    target.setStyle(null);
                });
            }
        });
    }

    @FXML
    private void handleThumb1Click(javafx.scene.input.MouseEvent e) {
        if (thumb1View.getImage() != null) heroImageView.setImage(thumb1View.getImage());
    }

    @FXML
    private void handleThumb2Click(javafx.scene.input.MouseEvent e) {
        if (thumb2View.getImage() != null) heroImageView.setImage(thumb2View.getImage());
    }

    @FXML
    private void handleThumb3Click(javafx.scene.input.MouseEvent e) {
        if (thumb3View.getImage() != null) heroImageView.setImage(thumb3View.getImage());
    }

    // allow gallery to request showing a particular hero index after loading
    public void showHeroImageIndex(int index) {
        switch (index) {
            case 0: if (heroImageView.getImage() != null) heroImageView.setImage(heroImageView.getImage()); break;
            case 1: if (thumb1View.getImage() != null) heroImageView.setImage(thumb1View.getImage()); break;
            case 2: if (thumb2View.getImage() != null) heroImageView.setImage(thumb2View.getImage()); break;
            default: break;
        }
    }

    private void animateSuccess() {
        if (heroImageView == null) return;
        ScaleTransition st = new ScaleTransition(Duration.millis(300), heroImageView);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(1.06);
        st.setToY(1.06);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }

    private void showToast(String message) {
        try {
            Label lbl = new Label(message);
            lbl.setStyle("-fx-background-color: rgba(40,160,67,0.95); -fx-text-fill: white; -fx-padding: 8px 12px; -fx-background-radius: 6px;");
            Popup popup = new Popup();
            popup.getContent().add(lbl);
            javafx.geometry.Bounds b = heroImageView.localToScreen(heroImageView.getBoundsInLocal());
            double x = b.getMinX() + b.getWidth() - 10;
            double y = b.getMinY() + 10;
            popup.show(heroImageView.getScene().getWindow(), x, y);
            PauseTransition pt = new PauseTransition(Duration.seconds(1.6));
            pt.setOnFinished(evt -> popup.hide());
            pt.play();
        } catch (Exception ignored) {}
    }

    private static javafx.scene.image.Image loadPlaceholderImage() {
        InputStream stream = AuctionDetailController.class.getResourceAsStream("/images/placeholder.png");
        if (stream == null) {
            throw new IllegalStateException("Missing resource: /images/placeholder.png");
        }
        return new javafx.scene.image.Image(stream);
    }

    @FXML
    private void handleCancelAuction() {
        System.out.println("Cancel auction clicked");
    }

    @FXML
    private void handleRelistAuction() {
        System.out.println("Relist auction clicked");
    }

    @FXML
    private void handleBackToGallery() {
        try {
            shutdown();
            ClientContext.getInstance().getViewLoader().loadView("gallery.fxml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
