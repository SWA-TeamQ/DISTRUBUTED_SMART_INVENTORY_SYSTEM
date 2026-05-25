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
    @FXML private Label timeLeftLabel;
    @FXML private Label highestBidderLabel;
    @FXML private Button placeBidButton;
    @FXML private javafx.scene.layout.HBox reconnectBanner;
    @FXML private Label reconnectLabel;

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
    @FXML private ProgressIndicator thumb2Spinner;
    @FXML private ProgressIndicator thumb3Spinner;
    @FXML private javafx.scene.control.TableView<com.auction.shared.models.Bid> bidHistoryTable;
    @FXML private javafx.scene.control.TableColumn<com.auction.shared.models.Bid, String> timeColumn;
    @FXML private javafx.scene.control.TableColumn<com.auction.shared.models.Bid, String> userColumn;
    @FXML private javafx.scene.control.TableColumn<com.auction.shared.models.Bid, Long> amountColumn;
    private final java.util.concurrent.Executor executor = java.util.concurrent.Executors.newCachedThreadPool();
    private static final javafx.scene.image.Image PLACEHOLDER_IMAGE = loadPlaceholderImage();
    private volatile long serverClockOffsetMillis = 0L;
    private Timeline countdownTimeline;
    // lazy thumbnail prefetch controls
    private volatile boolean thumb2Loaded = false;
    private volatile boolean thumb3Loaded = false;
    private PauseTransition thumb2HoverTimer;
    private PauseTransition thumb3HoverTimer;
    // track previous end-time to detect snipe extensions
    private volatile String prevEndTimeIso = null;

    @FXML
    public void initialize() {
        // configure bid history columns formatting
        try {
            if (timeColumn != null) {
                timeColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("timestamp"));
            }
            if (userColumn != null) {
                userColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("bidderUsername"));
            }
            if (amountColumn != null) {
                amountColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("amountCents"));
                amountColumn.setCellFactory(col -> new javafx.scene.control.TableCell<com.auction.shared.models.Bid, Long>() {
                    @Override
                    protected void updateItem(Long value, boolean empty) {
                        super.updateItem(value, empty);
                        if (empty || value == null) {
                            setText(null);
                        } else {
                            setText(com.auction.shared.Constants.formatCents(value));
                        }
                    }
                });
            }
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
        Platform.runLater(() -> {
            if (auctionTitleLabel != null) auctionTitleLabel.setText("Loading auction...");
            if (auctionDescriptionLabel != null) auctionDescriptionLabel.setText("Loading details...");
            if (currentBidLabel != null) currentBidLabel.setText("--");
            if (highestBidderLabel != null) highestBidderLabel.setText("Loading...");
            if (timeLeftLabel != null) timeLeftLabel.setText("--:--");
            if (bidHistoryTable != null) bidHistoryTable.getItems().clear();
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

                // load hero + primary thumb eagerly; lazily prefetch/load thumb2 and thumb3
                loadDetailThumbnail(auctionId, 0, heroImageView);
                loadDetailThumbnail(auctionId, 1, thumb1View);
                try {
                    if (thumb2View != null) {
                        thumb2Loaded = false;
                        thumb2HoverTimer = new PauseTransition(Duration.millis(300));
                        thumb2HoverTimer.setOnFinished(ev -> {
                            if (!thumb2Loaded) {
                                loadDetailThumbnail(auctionId, 2, thumb2View);
                                thumb2Loaded = true;
                            }
                        });
                        thumb2View.setOnMouseEntered(e -> { try { thumb2HoverTimer.playFromStart(); } catch (Exception ignored) {} });
                        thumb2View.setOnMouseExited(e -> { try { thumb2HoverTimer.stop(); } catch (Exception ignored) {} });
                    }
                    if (thumb3View != null) {
                        thumb3Loaded = false;
                        thumb3HoverTimer = new PauseTransition(Duration.millis(300));
                        thumb3HoverTimer.setOnFinished(ev -> {
                            if (!thumb3Loaded) {
                                loadDetailThumbnail(auctionId, 3, thumb3View);
                                thumb3Loaded = true;
                            }
                        });
                        thumb3View.setOnMouseEntered(e -> { try { thumb3HoverTimer.playFromStart(); } catch (Exception ignored) {} });
                        thumb3View.setOnMouseExited(e -> { try { thumb3HoverTimer.stop(); } catch (Exception ignored) {} });
                    }
                } catch (Exception ignored) {}

                pollingService.startPolling(auctionId, item -> {
                    // capture prior end-time to detect extensions
                    String priorEnd = this.currentItem == null ? this.prevEndTimeIso : this.currentItem.getEndTime();
                    this.currentItem = item;
                    Platform.runLater(() -> {
                        syncServerClockOffset(service);
                        updateUi(item);
                        hideReconnectBanner();
                        // refresh bid history whenever we receive an updated item
                        refreshBidHistory();

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

    private void refreshBidHistory() {
        try {
            var service = ClientContext.getInstance().getRmiProvider().getService();
            java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    return service.getBidHistory(currentAuctionId);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }, executor).thenAccept(list -> {
                if (list == null) return;
                Platform.runLater(() -> {
                    try {
                        if (bidHistoryTable != null) {
                            javafx.collections.ObservableList<com.auction.shared.models.Bid> items = javafx.collections.FXCollections.observableArrayList(list);
                            bidHistoryTable.setItems(items);
                        }
                    } catch (Exception ignored) {}
                });
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
        updateCountdownLabel(item);
        // Disable bid UI when auction is not ACTIVE
        try {
            boolean active = com.auction.shared.Constants.STATUS_ACTIVE.equals(item.getStatus());
            if (placeBidButton != null) placeBidButton.setDisable(!active);
            if (bidAmountField != null) bidAmountField.setDisable(!active);
            // also update bidder controls visibility if present
            if (placeBidButton != null && bidStatusLabel != null) {
                if (!active) {
                    bidStatusLabel.setText("Bidding closed");
                } else {
                    // clear status when active
                    if (bidStatusLabel.getText() != null && bidStatusLabel.getText().contains("Bidding closed")) {
                        bidStatusLabel.setText("");
                    }
                }
            }
        } catch (Exception ignored) {}
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
        timeLeftLabel.setText(formatTimeLeft(item.getEndTime()));
        updateCountdownVisuals(item);
    }

    private void updateCountdownVisuals(AuctionItem item) {
        try {
            if (timeLeftLabel == null || item == null || item.getEndTime() == null) return;
            java.time.Instant end = java.time.Instant.parse(item.getEndTime());
            long remainingSeconds = java.time.Duration.between(java.time.Instant.now().plusMillis(serverClockOffsetMillis), end).getSeconds();
            // update style classes for visual states
            setCountdownStateClass(remainingSeconds);
            // update accessibility tooltip + accessible text
            try {
                String tooltipText = remainingSeconds <= 0 ? "Ended" : formatTimeLeft(item.getEndTime());
                javafx.scene.control.Tooltip t = javafx.scene.control.Tooltip.installation == null ? null : null; // placeholder
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
                        });
                    }
                } else {
                    Platform.runLater(() -> {
                        updateUi(currentItem);
                        bidStatusLabel.setText("Failed: " + finalMsg);
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
            // mark loaded flags for lazy thumbs
            try {
                if (index == 2) thumb2Loaded = true;
                if (index == 3) thumb3Loaded = true;
            } catch (Exception ignored) {}
        });
    }

    @FXML
    private void handleThumb1Click(javafx.scene.input.MouseEvent e) {
        if (thumb1View.getImage() != null) heroImageView.setImage(thumb1View.getImage());
    }

    @FXML
    private void handleThumb2Click(javafx.scene.input.MouseEvent e) {
        if (thumb2View.getImage() != null) {
            heroImageView.setImage(thumb2View.getImage());
            return;
        }
        try {
            if (thumb2Spinner != null) Platform.runLater(() -> thumb2Spinner.setVisible(true));
            loadDetailThumbnail(currentAuctionId, 2, thumb2View);
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                long waited = 0L;
                while (waited < 2000) {
                    try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                    waited += 100;
                    if (thumb2View.getImage() != null) break;
                }
                Platform.runLater(() -> {
                    if (thumb2View.getImage() != null) heroImageView.setImage(thumb2View.getImage());
                    if (thumb2Spinner != null) thumb2Spinner.setVisible(false);
                });
            }, executor);
        } catch (Exception ignored) {}
    }

    @FXML
    private void handleThumb3Click(javafx.scene.input.MouseEvent e) {
        if (thumb3View.getImage() != null) {
            heroImageView.setImage(thumb3View.getImage());
            return;
        }
        try {
            if (thumb3Spinner != null) Platform.runLater(() -> thumb3Spinner.setVisible(true));
            loadDetailThumbnail(currentAuctionId, 3, thumb3View);
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                long waited = 0L;
                while (waited < 2000) {
                    try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                    waited += 100;
                    if (thumb3View.getImage() != null) break;
                }
                Platform.runLater(() -> {
                    if (thumb3View.getImage() != null) heroImageView.setImage(thumb3View.getImage());
                    if (thumb3Spinner != null) thumb3Spinner.setVisible(false);
                });
            }, executor);
        } catch (Exception ignored) {}
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
            ClientContext context = ClientContext.getInstance();
            String targetView = context.getPreviousViewName();
            if (targetView == null || targetView.isBlank()) {
                targetView = "gallery.fxml";
            }
            context.getViewLoader().loadView(targetView);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
