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
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.stage.Popup;
import com.auction.client.service.ThumbnailExecutor;

import java.io.IOException;
import java.io.InputStream;

public class AuctionDetailController {

    @FXML private Label auctionTitleLabel;
    @FXML private Label auctionDescriptionLabel;
    @FXML private Label currentBidLabel;
    @FXML private Label minIncrementPercentLabel;
    @FXML private Label nextMinimumBidLabel;
    @FXML private Label finalSaleLabel;
    @FXML private Label timeContextLabel;
    @FXML private Label timeLeftLabel;
    @FXML private Label endTimestampLabel;
    @FXML private Label highestBidderLabel;
    @FXML private Label recentBidsTitleLabel;
    @FXML private Label recentBidsSubtitleLabel;
    @FXML private Button placeBidButton;
    @FXML private Button backButton;
    @FXML private javafx.scene.layout.HBox reconnectBanner;
    @FXML private Label reconnectLabel;

    private PollingService pollingService;
    private int currentAuctionId = -1;
    private com.auction.shared.models.AuctionItem currentItem;
    @FXML private TextField bidAmountField;
    @FXML private Label inlineNextMinLabel;
    @FXML private Label bidStatusLabel;
    @FXML private ProgressIndicator bidSpinner;
    @FXML private javafx.scene.image.ImageView heroImageView;
    @FXML private javafx.scene.image.ImageView thumb1View;
    @FXML private javafx.scene.image.ImageView thumb2View;
    @FXML private javafx.scene.image.ImageView thumb3View;
    @FXML private javafx.scene.control.TableView<com.auction.shared.models.Bid> recentBidsTable;
    // initialize recent bids table cell factories only once to avoid repeated resets
    private volatile boolean recentBidsColumnsInitialized = false;
    // prevent overlapping network refreshes for recent bids
    private final java.util.concurrent.atomic.AtomicBoolean recentBidsRefreshInFlight = new java.util.concurrent.atomic.AtomicBoolean(false);
    @FXML private ProgressIndicator thumb2Spinner;
    @FXML private ProgressIndicator thumb3Spinner;
    private final java.util.concurrent.Executor executor = java.util.concurrent.Executors.newCachedThreadPool();
    private static final javafx.scene.image.Image PLACEHOLDER_IMAGE = loadPlaceholderImage();
    private volatile long serverClockOffsetMillis = 0L;
    private Timeline countdownTimeline;
    // lazy thumbnail prefetch controls
    @SuppressWarnings("unused")
    private volatile boolean thumb2Loaded = false;
    @SuppressWarnings("unused")
    private volatile boolean thumb3Loaded = false;
    private PauseTransition thumb2HoverTimer;
    private PauseTransition thumb3HoverTimer;
    // track previous end-time to detect snipe extensions
    private volatile String prevEndTimeIso = null;
    private String returnViewName;
    // ensure we only notify the user once when an auction ends
    private volatile boolean endNotificationShown = false;

    @FXML
    public void initialize() {
        updateBackButtonLabel();
        // debug log to show what return target values are when the detail view initializes
        try {
            System.out.println("[RTDAS] AuctionDetail.initialize: returnViewName=" + this.returnViewName + ", ClientContext.previousViewName=" + ClientContext.getInstance().getPreviousViewName());
        } catch (Exception ignored) {}

        try {
            int selectedAuctionId = ClientContext.getInstance().getCurrentAuctionId();
            if (selectedAuctionId >= 0) {
                loadAuction(selectedAuctionId);
            }
        } catch (Exception ignored) {}
    }

    public void loadAuction(int auctionId) {
        this.currentAuctionId = auctionId;
        this.endNotificationShown = false;
        updateBackButtonLabel();
        Platform.runLater(() -> {
            if (auctionTitleLabel != null) auctionTitleLabel.setText("Loading auction...");
            if (auctionDescriptionLabel != null) auctionDescriptionLabel.setText("Loading details...");
            if (currentBidLabel != null) currentBidLabel.setText("--");
            if (minIncrementPercentLabel != null) minIncrementPercentLabel.setText("--");
            if (nextMinimumBidLabel != null) nextMinimumBidLabel.setText("Next: --");
            if (finalSaleLabel != null) {
                finalSaleLabel.setText("");
                finalSaleLabel.setVisible(false);
                finalSaleLabel.setManaged(false);
            }
            if (inlineNextMinLabel != null) {
                inlineNextMinLabel.setText("");
                inlineNextMinLabel.setVisible(false);
                inlineNextMinLabel.setManaged(false);
            }
            if (highestBidderLabel != null) highestBidderLabel.setText("Loading...");
            if (timeLeftLabel != null) timeLeftLabel.setText("--:--");
            // keep the image area empty until the thumbnail fetch resolves
            if (heroImageView != null) {
                heroImageView.setImage(null);
                heroImageView.setVisible(false);
                heroImageView.setManaged(false);
            }
            if (thumb1View != null) {
                thumb1View.setImage(null);
                thumb1View.setVisible(false);
                thumb1View.setManaged(false);
            }
            if (thumb2View != null) {
                thumb2View.setImage(null);
                thumb2View.setVisible(false);
                thumb2View.setManaged(false);
            }
            if (thumb3View != null) {
                thumb3View.setImage(null);
                thumb3View.setVisible(false);
                thumb3View.setManaged(false);
            }
        });

        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                var service = ClientContext.getInstance().getRmiProvider().getService();
                AuctionItem initial = service.getAuctionById(auctionId);
                return new java.util.AbstractMap.SimpleEntry<>(service, initial);
            } catch (Exception e) {
                throw new java.util.concurrent.CompletionException(e);
            }
        }, executor).whenComplete((result, throwable) -> {
            if (throwable != null || result == null) {
                Platform.runLater(() -> {
                    if (auctionDescriptionLabel != null) {
                        auctionDescriptionLabel.setText("Unable to load auction details.");
                    }
                    showReconnectBanner(throwable);
                });
                return;
            }

            var service = result.getKey();
            AuctionItem initial = result.getValue();

            try {
                this.pollingService = new PollingService(service);
                syncServerClockOffset(service);
                this.currentItem = initial;
                this.prevEndTimeIso = initial == null ? null : initial.getEndTime();

                Platform.runLater(() -> {
                    updateUi(initial);
                    hideReconnectBanner();
                    startCountdownTicker();
                });

                // show all available images as explicit rail slots and include primary image in the rail
                // hero = image 0, thumb1 = image 0, thumb2 = image 1, thumb3 = image 2
                loadDetailThumbnail(auctionId, 0, heroImageView);
                if (thumb1View != null) {
                    thumb1View.setPickOnBounds(true);
                    thumb1View.setCursor(javafx.scene.Cursor.HAND);
                }
                if (thumb2View != null) {
                    thumb2View.setPickOnBounds(true);
                    thumb2View.setCursor(javafx.scene.Cursor.HAND);
                }
                if (thumb3View != null) {
                    thumb3View.setPickOnBounds(true);
                    thumb3View.setCursor(javafx.scene.Cursor.HAND);
                }
                // load rail: primary image first, then subsequent images
                loadDetailThumbnail(auctionId, 0, thumb1View);
                loadDetailThumbnail(auctionId, 1, thumb2View);
                loadDetailThumbnail(auctionId, 2, thumb3View);

                pollingService.startPolling(auctionId, item -> {
                    // capture prior end-time to detect extensions
                    String priorEnd = this.currentItem == null ? this.prevEndTimeIso : this.currentItem.getEndTime();
                    this.currentItem = item;
                    Platform.runLater(() -> {
                        syncServerClockOffset(service);
                        updateUi(item);
                        // refresh recent bids list on each poll
                        refreshRecentBids();
                        hideReconnectBanner();
                        // if end time was extended on the server, notify user
                        try {
                            if (priorEnd != null && item != null && item.getEndTime() != null) {
                                java.time.Instant prior = java.time.Instant.parse(priorEnd);
                                java.time.Instant nowEnd = java.time.Instant.parse(item.getEndTime());
                                if (nowEnd.isAfter(prior)) {
                                    showToast("Timer extended!");
                                    highlightCountdownExtension();
                                }
                            }
                        } catch (Exception ignored) {}
                        // update prevEndTimeIso
                        prevEndTimeIso = item == null ? null : item.getEndTime();
                    });
                }, t -> {
                    // show reconnect banner when repeated failures occur
                    Platform.runLater(() -> showReconnectBanner(t));
                });

                // initial load of recent bids
                refreshRecentBids();

                // Pause/resume polling when window focus changes to reduce load
                Platform.runLater(() -> {
                    try {
                        if (heroImageView != null && heroImageView.getScene() != null && heroImageView.getScene().getWindow() != null) {
                            var window = heroImageView.getScene().getWindow();
                            window.focusedProperty().addListener((obs, oldV, newV) -> {
                                if (pollingService != null) {
                                    if (Boolean.FALSE.equals(newV)) {
                                        pollingService.pause();
                                    } else {
                                        pollingService.resume();
                                    }
                                }
                            });
                        }
                    } catch (Exception ignored) {}
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showReconnectBanner(e));
            }
        });
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
        updateMinimumBidLabels(item);
        highestBidderLabel.setText(item.getHighestBidderUsername() == null ? "N/A" : item.getHighestBidderUsername());
        updateCountdownLabel(item);
        updateRecentBidsHeader(item);
        // Disable bid UI when auction is not ACTIVE
        try {
            boolean active = com.auction.shared.Constants.STATUS_ACTIVE.equals(item.getStatus());
            if (placeBidButton != null) placeBidButton.setDisable(!active);
            if (bidAmountField != null) bidAmountField.setDisable(!active);
            // also update bidder controls visibility if present
            if (placeBidButton != null && bidStatusLabel != null) {
                if (!active) {
                    if (com.auction.shared.Constants.STATUS_SCHEDULED.equals(item.getStatus())) {
                        bidStatusLabel.setText("Auction has not started yet");
                    } else {
                        bidStatusLabel.setText("Bidding closed");
                    }
                } else {
                    // clear status when active
                    if (bidStatusLabel.getText() != null && (bidStatusLabel.getText().contains("Bidding closed") || bidStatusLabel.getText().contains("not started"))) {
                        bidStatusLabel.setText("");
                    }
                    // reset end-notification when auction becomes active again
                    endNotificationShown = false;
                }
            }
        } catch (Exception ignored) {}
    }

    private void updateRecentBidsHeader(AuctionItem item) {
        if (item == null) return;
        boolean sold = com.auction.shared.Constants.STATUS_SOLD.equals(item.getStatus());
        if (recentBidsTitleLabel != null) {
            recentBidsTitleLabel.setText(sold ? "Bid History (read-only)" : "Bid History");
        }
        if (recentBidsSubtitleLabel != null) {
            recentBidsSubtitleLabel.setText(sold
                ? "Historical bids for this sold auction are shown below and cannot be modified."
                : "Chronological history for the selected auction.");
        }
        if (recentBidsTable != null) {
            recentBidsTable.setEditable(false);
            recentBidsTable.setFocusTraversable(false);
        }
    }

    private void updateMinimumBidLabels(AuctionItem item) {
        if (item == null) return;

        double percent = item.getMinIncrementPercent();
        if (percent <= 0d) {
            percent = com.auction.shared.Constants.MIN_BID_INCREMENT_PERCENT;
        }

        // Always show the configured percentage (or default) for all statuses
        if (minIncrementPercentLabel != null) {
            minIncrementPercentLabel.setText(String.format("%.1f%%", percent * 100.0));
            minIncrementPercentLabel.setVisible(true);
            minIncrementPercentLabel.setManaged(true);
        }

        // Next-minimum logic varies by status, but the inline helper should stay visible
        // for any non-sold auction so the place-bid panel always shows a reference amount.
        String status = item.getStatus();
        long referenceBase = Math.max(0L, item.getCurrentBidCents());
        String inlinePrefix = "Minimum bid: ";
        if (com.auction.shared.Constants.STATUS_ACTIVE.equals(status)) {
            long nextMinimumBid;
            if (item.getHighestBidderUsername() == null) {
                // First bid may start at the auction's opening price.
                nextMinimumBid = Math.max(0L, item.getStartingPriceCents());
                inlinePrefix = "Starting bid: ";
            } else {
                long incrementCents = Math.max(1L, Math.round(referenceBase * percent));
                nextMinimumBid = referenceBase + incrementCents;
            }
            if (nextMinimumBidLabel != null) {
                nextMinimumBidLabel.setText("Next: " + com.auction.shared.Constants.formatCents(nextMinimumBid));
                nextMinimumBidLabel.setVisible(true);
                nextMinimumBidLabel.setManaged(true);
            }
        } else if (com.auction.shared.Constants.STATUS_SCHEDULED.equals(status)) {
            referenceBase = Math.max(0L, item.getStartingPriceCents());
            inlinePrefix = "Starts at: ";
            long incrementCents = Math.max(1L, Math.round(referenceBase * percent));
            long nextMinimumBid = referenceBase + incrementCents;
            if (nextMinimumBidLabel != null) {
                nextMinimumBidLabel.setText("Next: " + com.auction.shared.Constants.formatCents(nextMinimumBid));
                nextMinimumBidLabel.setVisible(true);
                nextMinimumBidLabel.setManaged(true);
            }
        } else {
            if (nextMinimumBidLabel != null) {
                nextMinimumBidLabel.setText("");
                nextMinimumBidLabel.setVisible(false);
                nextMinimumBidLabel.setManaged(false);
            }
        }

        if (!com.auction.shared.Constants.STATUS_SOLD.equals(status) && inlineNextMinLabel != null) {
            long referenceNext;
            if (com.auction.shared.Constants.STATUS_ACTIVE.equals(status) && item.getHighestBidderUsername() == null) {
                referenceNext = Math.max(0L, item.getStartingPriceCents());
            } else {
                long referenceIncrement = Math.max(1L, Math.round(referenceBase * percent));
                referenceNext = referenceBase + referenceIncrement;
            }
            inlineNextMinLabel.setText(inlinePrefix + com.auction.shared.Constants.formatCents(referenceNext));
            inlineNextMinLabel.setVisible(true);
            inlineNextMinLabel.setManaged(true);
        } else if (inlineNextMinLabel != null) {
            inlineNextMinLabel.setText("");
            inlineNextMinLabel.setVisible(false);
            inlineNextMinLabel.setManaged(false);
        }

        // Final sale handling: show sold price if status is SOLD
        if (com.auction.shared.Constants.STATUS_SOLD.equals(item.getStatus())) {
            if (finalSaleLabel != null) {
                finalSaleLabel.setText("Sold for " + com.auction.shared.Constants.formatCents(item.getCurrentBidCents()));
                finalSaleLabel.setVisible(true);
                finalSaleLabel.setManaged(true);
            }
            // hide nextMinimum label when sold
            if (nextMinimumBidLabel != null) {
                nextMinimumBidLabel.setText("");
                nextMinimumBidLabel.setVisible(false);
                nextMinimumBidLabel.setManaged(false);
            }
        } else {
            if (finalSaleLabel != null) {
                finalSaleLabel.setText("");
                finalSaleLabel.setVisible(false);
                finalSaleLabel.setManaged(false);
            }
        }
    }

    private String formatTimeLeft(String endTimeIso) {
        return formatTimeLeft(java.time.Instant.now().plusMillis(serverClockOffsetMillis), endTimeIso);
    }

    static String formatTimeLeft(java.time.Instant adjustedNow, String endTimeIso) {
        if (endTimeIso == null || endTimeIso.isBlank()) return "--:--";
        try {
            java.time.Instant end = java.time.Instant.parse(endTimeIso);
            java.time.Duration d = java.time.Duration.between(adjustedNow, end);
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

    private void updateCountdownLabel(AuctionItem item) {
        if (timeLeftLabel == null || item == null) return;
        if (com.auction.shared.Constants.STATUS_SCHEDULED.equals(item.getStatus())) {
            if (timeContextLabel != null) timeContextLabel.setText("Starts In");
            String startIso = item.getStartTime();
            if (startIso == null || startIso.isBlank()) {
                timeLeftLabel.setText("Awaiting manual start");
            } else {
                String startsIn = formatTimeLeft(startIso);
                if ("Ended".equals(startsIn)) {
                    timeLeftLabel.setText("Awaiting manual start");
                } else {
                    timeLeftLabel.setText(startsIn);
                }
            }
        } else {
            if (timeContextLabel != null) timeContextLabel.setText("Time Left");
            String formatted = formatTimeLeft(item.getEndTime());
            if ("Ended".equals(formatted)) {
                // show ended indicator and timestamp
                timeLeftLabel.setText("Ended");
                try {
                    if (endTimestampLabel != null && item.getEndTime() != null && !item.getEndTime().isBlank()) {
                        java.time.Instant end = java.time.Instant.parse(item.getEndTime());
                        java.time.ZonedDateTime zdt = java.time.ZonedDateTime.ofInstant(end, java.time.ZoneId.systemDefault());
                        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");
                        endTimestampLabel.setText("Ended at: " + zdt.format(fmt));
                        endTimestampLabel.setVisible(true);
                        endTimestampLabel.setManaged(true);
                    }
                } catch (Exception ignored) {
                    if (endTimestampLabel != null) {
                        endTimestampLabel.setVisible(false);
                        endTimestampLabel.setManaged(false);
                    }
                }
                // notify once when auction transitions to ended
                try {
                    if (!endNotificationShown && item != null) {
                        endNotificationShown = true;
                        String winner = item.getHighestBidderUsername();
                        long price = item.getCurrentBidCents();
                        if (winner == null || winner.isBlank()) {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Auction Ended");
                            alert.setHeaderText("Auction ended — no bids were placed");
                            alert.setContentText("This auction closed without any bids.");
                            alert.showAndWait();
                            if (bidStatusLabel != null) bidStatusLabel.setText("Auction ended — no winner");
                        } else {
                            String msg = "Auction ended! Congratulations! user " + winner + " has won the auction at " + com.auction.shared.Constants.formatCents(price);
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Auction Ended");
                            alert.setHeaderText("Auction ended — Winner: " + winner);
                            alert.setContentText("Sold for " + com.auction.shared.Constants.formatCents(price));
                            alert.showAndWait();
                            if (bidStatusLabel != null) bidStatusLabel.setText("Auction ended — Winner: " + winner + " at " + com.auction.shared.Constants.formatCents(price));
                        }
                    }
                } catch (Exception ignored) {}
            } else {
                timeLeftLabel.setText(formatted);
                if (endTimestampLabel != null) {
                    endTimestampLabel.setVisible(false);
                    endTimestampLabel.setManaged(false);
                }
            }
        }
        updateCountdownVisuals(item);
    }

    private void updateCountdownVisuals(AuctionItem item) {
        try {
            if (timeLeftLabel == null || item == null) return;
            long remainingSeconds;
            if (com.auction.shared.Constants.STATUS_SCHEDULED.equals(item.getStatus()) && item.getStartTime() != null && !item.getStartTime().isBlank()) {
                java.time.Instant start = java.time.Instant.parse(item.getStartTime());
                remainingSeconds = java.time.Duration.between(java.time.Instant.now().plusMillis(serverClockOffsetMillis), start).getSeconds();
            } else {
                if (item.getEndTime() == null) return;
                java.time.Instant end = java.time.Instant.parse(item.getEndTime());
                remainingSeconds = java.time.Duration.between(java.time.Instant.now().plusMillis(serverClockOffsetMillis), end).getSeconds();
            }
            // update style classes for visual states
            setCountdownStateClass(remainingSeconds);
            // update accessibility tooltip + accessible text
            try {
                String tooltipText = remainingSeconds <= 0 ? "Ended" : formatTimeLeft(item.getEndTime());
                javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(tooltipText);
                try { javafx.scene.control.Tooltip.install(timeLeftLabel, tooltip); } catch (Exception ignored) {}
                timeLeftLabel.setAccessibleText(tooltipText);
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    private void setCountdownStateClass(long remainingSeconds) {
        try {
            if (timeLeftLabel == null) return;
            timeLeftLabel.getStyleClass().removeAll("countdown-normal", "countdown-warning", "countdown-urgent", "countdown-ended");
            if (remainingSeconds <= 0) {
                timeLeftLabel.getStyleClass().add("countdown-ended");
                timeLeftLabel.setAccessibleText("Time left: Ended");
            } else if (remainingSeconds <= 30) {
                timeLeftLabel.getStyleClass().add("countdown-urgent");
                timeLeftLabel.setAccessibleText("Time left: less than 30 seconds");
            } else if (remainingSeconds <= 60) {
                timeLeftLabel.getStyleClass().add("countdown-warning");
                timeLeftLabel.setAccessibleText("Time left: less than one minute");
            } else {
                timeLeftLabel.getStyleClass().add("countdown-normal");
                timeLeftLabel.setAccessibleText("Time left: more than one minute");
            }
            // Tooltip for screen readers and mouse users
            try {
                javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(timeLeftLabel.getAccessibleText());
                javafx.scene.control.Tooltip.install(timeLeftLabel, tooltip);
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    private void highlightCountdownExtension() {
        try {
            if (timeLeftLabel == null) return;
            // brief scale + glow animation to draw attention
            ScaleTransition st = new ScaleTransition(Duration.millis(220), timeLeftLabel);
            st.setFromX(1.0);
            st.setFromY(1.0);
            st.setToX(1.08);
            st.setToY(1.08);
            st.setAutoReverse(true);
            st.setCycleCount(2);
            st.play();
        } catch (Exception ignored) {}
    }

    private void startCountdownTicker() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), evt -> {
            if (currentItem != null) {
                updateCountdownLabel(currentItem);
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void syncServerClockOffset(com.auction.shared.interfaces.IAuctionService service) {
        try {
            String serverTimeIso = service.serverTime();
            if (serverTimeIso != null && !serverTimeIso.isBlank()) {
                java.time.Instant serverInstant = java.time.Instant.parse(serverTimeIso);
                serverClockOffsetMillis = serverInstant.toEpochMilli() - System.currentTimeMillis();
            }
        } catch (Exception ignored) {
            // keep the last known offset if the server time request fails
        }
    }

    public void shutdown() {
        if (pollingService != null) pollingService.shutdown();
        if (countdownTimeline != null) countdownTimeline.stop();
        try { if (thumb2HoverTimer != null) thumb2HoverTimer.stop(); } catch (Exception ignored) {}
        try { if (thumb3HoverTimer != null) thumb3HoverTimer.stop(); } catch (Exception ignored) {}
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

        // Show an immediate response for the common self-bid case before the server round-trip.
        try {
            String username = ClientContext.getInstance().getUsername();
            if (currentItem != null && username != null && username.equals(currentItem.getSellerUsername())) {
                bidStatusLabel.setText("You cannot bid on your own auction.");
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Invalid Bid");
                alert.setHeaderText("Cannot bid on your own item");
                alert.setContentText("You are the seller of this auction.");
                alert.showAndWait();
                return;
            }
        } catch (Exception ignored) {}

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
                try {
                    AuctionItem refreshed = service.getAuctionById(currentAuctionId);
                    if (refreshed != null) {
                        currentItem = refreshed;
                        Platform.runLater(() -> updateUi(refreshed));
                    }
                } catch (Exception refreshError) {
                    refreshError.printStackTrace();
                }
            } catch (Exception e) {
                // parse server-side AuctionException if present and show Alerts for parity with docs
                String userMsg = "Failed to place bid";
                Throwable cause = e;
                com.auction.shared.exceptions.AuctionException foundEx = null;
                while (cause != null) {
                    if (cause instanceof com.auction.shared.exceptions.AuctionException) {
                        foundEx = (com.auction.shared.exceptions.AuctionException) cause;
                        userMsg = foundEx.getMessage();
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
                // Decide whether to show an Alert dialog for parity with docs
                if (foundEx != null) {
                    com.auction.shared.exceptions.AuctionException ex = foundEx;
                    if (ex instanceof com.auction.shared.exceptions.StaleDataException) {
                        Platform.runLater(() -> {
                            updateUi(currentItem);
                            bidStatusLabel.setText("Failed: " + finalMsg);
                            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
                            a.setTitle("Bid Out Of Date");
                            a.setHeaderText("Price changed on server");
                            a.setContentText(finalMsg + " Please refresh and try again.");
                            a.showAndWait();
                        });
                    } else if (ex instanceof com.auction.shared.exceptions.SelfBidException) {
                        Platform.runLater(() -> {
                            updateUi(currentItem);
                            bidStatusLabel.setText("Failed: " + finalMsg);
                            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                            a.setTitle("Invalid Bid");
                            a.setHeaderText("Cannot bid on your own item");
                            a.setContentText(finalMsg);
                            a.showAndWait();
                        });
                    } else if (ex instanceof com.auction.shared.exceptions.DuplicateBidException) {
                        Platform.runLater(() -> {
                            updateUi(currentItem);
                            bidStatusLabel.setText("Failed: " + finalMsg);
                            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                            a.setTitle("Already Highest Bidder");
                            a.setHeaderText("You're already the highest bidder");
                            a.setContentText(finalMsg);
                            a.showAndWait();
                        });
                    } else {
                        Platform.runLater(() -> {
                            updateUi(currentItem);
                            bidStatusLabel.setText("Failed: " + finalMsg);
                            Alert a = new Alert(Alert.AlertType.ERROR);
                            a.setTitle("Bid Failed");
                            a.setHeaderText("Bid could not be placed");
                            a.setContentText(finalMsg);
                            a.showAndWait();
                        });
                    }
                } else {
                    Platform.runLater(() -> {
                        updateUi(currentItem);
                        bidStatusLabel.setText("Failed: " + finalMsg);
                        Alert a = new Alert(Alert.AlertType.ERROR);
                        a.setTitle("Bid Failed");
                        a.setHeaderText("Bid could not be placed");
                        a.setContentText(finalMsg);
                        a.showAndWait();
                    });
                }
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
        Platform.runLater(() -> {
            if (target != null) {
                target.setImage(null);
                target.setVisible(false);
                target.setManaged(false);
            }
        });

        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                var service = ClientContext.getInstance().getRmiProvider().getService();
                byte[] bytes = service.getThumbnail(auctionId, index);
                if (bytes == null || bytes.length == 0) {
                    bytes = service.getFullImage(auctionId, index);
                }
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
                    target.setVisible(true);
                    target.setManaged(true);
                    target.setStyle(null);
                });
            } else {
                Platform.runLater(() -> {
                    target.setImage(PLACEHOLDER_IMAGE);
                    target.setVisible(true);
                    target.setManaged(true);
                    target.setStyle(null);
                });
            }
            // mark loaded flags for lazy thumbs
            try {
                if (index == 2) thumb2Loaded = true;
                if (index == 3) thumb3Loaded = true;
            } catch (Exception ignored) {}
        });
    }

    @FXML
    private void handleThumb1Click(javafx.scene.input.MouseEvent e) {
        if (thumb1View.getImage() != null && thumb1View.getImage() != PLACEHOLDER_IMAGE) {
            heroImageView.setImage(thumb1View.getImage());
            heroImageView.setVisible(true);
            heroImageView.setManaged(true);
            return;
        }
        promoteThumbnailToHero(0);
    }

    @FXML
    private void handleThumb2Click(javafx.scene.input.MouseEvent e) {
        if (thumb2View.getImage() != null && thumb2View.getImage() != PLACEHOLDER_IMAGE) {
            heroImageView.setImage(thumb2View.getImage());
            heroImageView.setVisible(true);
            heroImageView.setManaged(true);
            return;
        }
        promoteThumbnailToHero(1);
    }

    @FXML
    private void handleThumb3Click(javafx.scene.input.MouseEvent e) {
        if (thumb3View.getImage() != null && thumb3View.getImage() != PLACEHOLDER_IMAGE) {
            heroImageView.setImage(thumb3View.getImage());
            heroImageView.setVisible(true);
            heroImageView.setManaged(true);
            return;
        }
        promoteThumbnailToHero(2);
    }

    // allow gallery to request showing a particular hero index after loading
    public void showHeroImageIndex(int index) {
        try {
            System.out.println("[RTDAS][Detail] showHeroImageIndex requested index=" + index + ", currentAuctionId=" + this.currentAuctionId + ", returnViewName=" + this.returnViewName);
        } catch (Exception ignored) {}
        if (heroImageView == null) return;
        switch (index) {
            case 0:
                promoteThumbnailToHero(0);
                break;
            case 1:
                promoteThumbnailToHero(1);
                break;
            case 2:
                promoteThumbnailToHero(2);
                break;
            default:
                promoteThumbnailToHero(0);
                break;
        }
    }

    private void promoteThumbnailToHero(int index) {
        try {
            System.out.println("[RTDAS][Detail] promoteThumbnailToHero called with index=" + index + ", currentAuctionId=" + this.currentAuctionId);
        } catch (Exception ignored) {}
        // map requested image index to the rail image view so promotion uses the rail slots
        javafx.scene.image.ImageView targetThumb = switch (index) {
            case 0 -> thumb1View;
            case 1 -> thumb2View;
            case 2 -> thumb3View;
            default -> null;
        };

        try {
            String tname = targetThumb == thumb1View ? "thumb1View" : targetThumb == thumb2View ? "thumb2View" : targetThumb == thumb3View ? "thumb3View" : "null";
            System.out.println("[RTDAS][Detail] promoteThumbnailToHero mapping: index=" + index + ", target=" + tname);
        } catch (Exception ignored) {}
        if (targetThumb == null || heroImageView == null) return;

        if (targetThumb.getImage() != null && targetThumb.getImage() != PLACEHOLDER_IMAGE) {
            heroImageView.setImage(targetThumb.getImage());
            heroImageView.setVisible(true);
            heroImageView.setManaged(true);
            return;
        }

        // If the source image has not loaded yet, load it now and promote it into the hero once available.
        loadDetailThumbnail(currentAuctionId, index, targetThumb);
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            long waited = 0L;
            while (waited < 2500) {
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                waited += 100;
                if (targetThumb.getImage() != null && targetThumb.getImage() != PLACEHOLDER_IMAGE) {
                    break;
                }
            }
            Platform.runLater(() -> {
                if (targetThumb.getImage() != null && targetThumb.getImage() != PLACEHOLDER_IMAGE) {
                    heroImageView.setImage(targetThumb.getImage());
                    heroImageView.setVisible(true);
                    heroImageView.setManaged(true);
                }
            });
        }, executor);
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
        shutdown();
        ClientContext context = ClientContext.getInstance();
        // defensive logging to help debug runtime issues reported by users
        System.out.println("[RTDAS] AuctionDetail.handleBackToGallery called; previousViewName=" + ClientContext.getInstance().getPreviousViewName() + ", returnViewName=" + this.returnViewName);
        String targetView = resolveTargetViewName();
        if (targetView == null || targetView.isBlank()) {
            targetView = "gallery.fxml";
        }
        context.setCurrentAuctionId(-1);
        context.setPreviousViewName(targetView);
        System.out.println("[RTDAS] AuctionDetail navigating back to: " + targetView);
        String finalTargetView = targetView;
        Platform.runLater(() -> {
            try {
                context.getViewLoader().loadView(finalTargetView);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @FXML
    private void handleOpenBidHistoryPage() {
        try {
            ClientContext context = ClientContext.getInstance();
            context.getViewLoader().loadView("auction_bid_history.fxml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Load recent bids asynchronously and update the `recentBidsTable`. */
    private void refreshRecentBids() {
        if (recentBidsTable == null || currentAuctionId < 0) return;
        // avoid overlapping refreshes
        if (!recentBidsRefreshInFlight.compareAndSet(false, true)) return;
        com.auction.client.service.BidHistoryService.loadBidHistoryAsync(currentAuctionId)
            .whenComplete((list, t) -> {
                recentBidsRefreshInFlight.set(false);
                if (t != null) return; // ignore failures; reconnect banner handles polling errors
                java.util.List<com.auction.shared.models.Bid> bids = list == null ? java.util.List.of() : list;
                // sort by amount descending then timestamp descending, limit to 10
                final java.util.List<com.auction.shared.models.Bid> sortedBids = bids.stream()
                    .sorted(java.util.Comparator.comparingLong(com.auction.shared.models.Bid::getAmountCents).reversed()
                        .thenComparing((b1, b2) -> {
                            try {
                                java.time.Instant i1 = java.time.Instant.parse(b1.getTimestamp());
                                java.time.Instant i2 = java.time.Instant.parse(b2.getTimestamp());
                                return i2.compareTo(i1);
                            } catch (Exception e) { return 0; }
                        }))
                    .limit(10)
                    .toList();

                Platform.runLater(() -> {
                    recentBidsTable.getItems().setAll(sortedBids);
                    // initialize columns and cell factories only once to avoid UI churn
                    try {
                        if (!recentBidsColumnsInitialized && recentBidsTable.getColumns().size() >= 3) {
                            recentBidsColumnsInitialized = true;
                            @SuppressWarnings("unchecked")
                            javafx.scene.control.TableColumn<com.auction.shared.models.Bid, String> timeCol = (javafx.scene.control.TableColumn<com.auction.shared.models.Bid, String>) recentBidsTable.getColumns().get(0);
                            timeCol.setCellFactory(new javafx.util.Callback<javafx.scene.control.TableColumn<com.auction.shared.models.Bid,String>, javafx.scene.control.TableCell<com.auction.shared.models.Bid,String>>() {
                                @Override
                                public javafx.scene.control.TableCell<com.auction.shared.models.Bid,String> call(javafx.scene.control.TableColumn<com.auction.shared.models.Bid,String> col) {
                                    return new javafx.scene.control.TableCell<com.auction.shared.models.Bid, String>() {
                                        private final java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");
                                        @Override protected void updateItem(String item, boolean empty) {
                                            super.updateItem(item, empty);
                                            if (empty || item == null) { setText(null); return; }
                                            try {
                                                java.time.Instant inst = java.time.Instant.parse(item);
                                                java.time.ZonedDateTime zdt = java.time.ZonedDateTime.ofInstant(inst, java.time.ZoneId.systemDefault());
                                                setText(zdt.format(fmt));
                                            } catch (Exception e) { setText(item); }
                                        }
                                    };
                                }
                            });

                            @SuppressWarnings("unchecked")
                            javafx.scene.control.TableColumn<com.auction.shared.models.Bid, String> userCol = (javafx.scene.control.TableColumn<com.auction.shared.models.Bid, String>) recentBidsTable.getColumns().get(1);
                            userCol.setCellFactory(new javafx.util.Callback<javafx.scene.control.TableColumn<com.auction.shared.models.Bid,String>, javafx.scene.control.TableCell<com.auction.shared.models.Bid,String>>() {
                                @Override
                                public javafx.scene.control.TableCell<com.auction.shared.models.Bid,String> call(javafx.scene.control.TableColumn<com.auction.shared.models.Bid,String> col) {
                                    return new javafx.scene.control.TableCell<com.auction.shared.models.Bid, String>() {
                                        @Override protected void updateItem(String item, boolean empty) {
                                            super.updateItem(item, empty);
                                            setText(empty || item == null ? null : item);
                                        }
                                    };
                                }
                            });

                            @SuppressWarnings("unchecked")
                            javafx.scene.control.TableColumn<com.auction.shared.models.Bid, Number> amtCol = (javafx.scene.control.TableColumn<com.auction.shared.models.Bid, Number>) recentBidsTable.getColumns().get(2);
                            amtCol.setCellFactory(new javafx.util.Callback<javafx.scene.control.TableColumn<com.auction.shared.models.Bid,Number>, javafx.scene.control.TableCell<com.auction.shared.models.Bid,Number>>() {
                                @Override
                                public javafx.scene.control.TableCell<com.auction.shared.models.Bid,Number> call(javafx.scene.control.TableColumn<com.auction.shared.models.Bid,Number> col) {
                                    return new javafx.scene.control.TableCell<com.auction.shared.models.Bid, Number>() {
                                        @Override protected void updateItem(Number item, boolean empty) {
                                            super.updateItem(item, empty);
                                            if (empty || item == null) { setText(null); return; }
                                            try {
                                                long cents = item.longValue();
                                                setText(com.auction.shared.Constants.formatCents(cents));
                                            } catch (Exception e) { setText(item.toString()); }
                                        }
                                    };
                                }
                            });
                        }
                    } catch (Exception ignored) {}
                });
            });
    }

    public void setReturnViewName(String returnViewName) {
        this.returnViewName = returnViewName;
        // keep shared context consistent for other callers that may read previousViewName
        try {
            ClientContext.getInstance().setPreviousViewName(returnViewName);
        } catch (Exception ignored) {}
        updateBackButtonLabel();
    }

    private String resolveTargetViewName() {
        if (returnViewName != null && !returnViewName.isBlank()) {
            return returnViewName;
        }
        String prev = ClientContext.getInstance().getPreviousViewName();
        if (prev != null && !prev.isBlank()) return prev;
        // defensive default: return to dashboard if nothing is set
        return "user_dashboard.fxml";
    }

    private void updateBackButtonLabel() {
        try {
            if (backButton == null) return;
            // ensure the button is visible and participates in layout
            backButton.setVisible(true);
            backButton.setManaged(true);
            String targetView = resolveTargetViewName();
            if (targetView == null || targetView.isBlank()) {
                backButton.setText("Back");
                return;
            }

            String normalized = targetView.toLowerCase();
            if (normalized.contains("gallery")) {
                backButton.setText("Back to Gallery");
            } else if (normalized.contains("dashboard")) {
                backButton.setText("Back to Dashboard");
            } else {
                backButton.setText("Back");
            }
        } catch (Exception ignored) {}
    }

    /**
     * Show a reconnect banner with an optional Throwable message. Safe to call from FX thread.
     */
    public void showReconnectBanner(Throwable t) {
        try {
            if (reconnectBanner != null) {
                String msg = "Disconnected — retrying...";
                if (t != null && t.getMessage() != null) msg = "Disconnected — " + t.getMessage();
                reconnectLabel.setText(msg);
                reconnectBanner.setVisible(true);
                reconnectBanner.setManaged(true);
            }
        } catch (Exception ignored) {}
    }

    /** Hide the reconnect banner. Safe to call from FX thread. */
    public void hideReconnectBanner() {
        try {
            if (reconnectBanner != null) {
                reconnectBanner.setVisible(false);
                reconnectBanner.setManaged(false);
            }
        } catch (Exception ignored) {}
    }
}
