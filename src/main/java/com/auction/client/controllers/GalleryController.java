package com.auction.client.controllers;

import com.auction.client.core.ClientContext;
import com.auction.shared.models.AuctionItem;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import com.auction.client.service.ThumbnailExecutor;

public class GalleryController {

    @FXML private FlowPane auctionFlow;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ChoiceBox<String> sortChoice;
    @FXML private Label auctionCountLabel;

    private final Map<String, Image> thumbnailCache = new ConcurrentHashMap<>();
    private static final Image PLACEHOLDER_IMAGE = loadPlaceholderImage();
    private java.util.List<AuctionItem> allAuctions = java.util.List.of();
    // Polling executor for live refresh (docs require 2s polling)
    private ScheduledExecutorService pollExecutor;
    private ScheduledFuture<?> pollTask;
    private static final long POLL_INTERVAL_SECONDS = 2L;

    @FXML
    public void initialize() {
        try {
            if (sortChoice != null && sortChoice.getItems().isEmpty()) {
                sortChoice.getItems().addAll("Newest", "Price: Low → High", "Price: High → Low");
                sortChoice.setValue("Newest");
                sortChoice.getSelectionModel().selectedItemProperty().addListener((a, b, c) -> fetchAndRenderAuctionsAsync());
            }
            if (categoryCombo != null) {
                categoryCombo.getItems().setAll("All");
                categoryCombo.setValue("All");
                categoryCombo.setOnAction(evt -> fetchAndRenderAuctionsAsync());
            }
            renderLoadingState();
            loadCategoriesThenQueryAsync();
        } catch (Exception ex) {
            ex.printStackTrace();
            Platform.runLater(() -> {
                if (auctionCountLabel != null) {
                    auctionCountLabel.setText("Failed to initialize gallery");
                }
            });
        } finally {
            // start the periodic polling after attempting initial load
            startPolling();
        }
    }

    @FXML
    private void handleRefresh() {
        renderLoadingState();
        loadCategoriesThenQueryAsync();
    }

    @FXML
    private void handleSearch() {
        renderLoadingState();
        fetchAndRenderAuctionsAsync();
    }

    private void loadCategoriesThenQueryAsync() {
        CompletableFuture.supplyAsync(() -> {
            try {
                var service = ClientContext.getInstance().getRmiProvider().getService();
                return service.getActiveAuctions();
            } catch (Exception e) {
                e.printStackTrace();
                return java.util.List.<AuctionItem>of();
            }
        }, ThumbnailExecutor.getExecutor()).thenAccept(items -> {
            allAuctions = (items == null) ? java.util.List.of() : items;
            var cats = allAuctions.stream()
                    .map(AuctionItem::getCategory)
                    .filter(c -> c != null && !c.isBlank())
                    .map(String::trim)
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
            Platform.runLater(() -> {
                if (categoryCombo != null) {
                    String previous = categoryCombo.getValue();
                    categoryCombo.getItems().clear();
                    categoryCombo.getItems().add("All");
                    categoryCombo.getItems().addAll(cats);
                    if (previous != null && categoryCombo.getItems().contains(previous)) {
                        categoryCombo.setValue(previous);
                    } else {
                        categoryCombo.setValue("All");
                    }
                }
                fetchAndRenderAuctionsAsync();
            });
        });
    }

    private void fetchAndRenderAuctionsAsync() {
        String query = searchField == null ? null : searchField.getText();
        String selectedCategory = categoryCombo == null ? null : categoryCombo.getValue();
        if ("All".equals(selectedCategory)) selectedCategory = null;
        String sortBy = mapSortToServer(sortChoice == null ? null : sortChoice.getValue());

        final String q = query;
        final String c = selectedCategory;
        final String s = sortBy;

        CompletableFuture.supplyAsync(() -> {
            try {
                var service = ClientContext.getInstance().getRmiProvider().getService();
                return service.searchActiveAuctions(q, c, s);
            } catch (Exception e) {
                e.printStackTrace();
                return fallbackFilterAndSort(allAuctions, q, c, s);
            }
        }, ThumbnailExecutor.getExecutor()).thenAccept(items -> Platform.runLater(() -> renderAuctions(items == null ? java.util.List.of() : items)));
    }

    private java.util.List<AuctionItem> fallbackFilterAndSort(java.util.List<AuctionItem> base, String query, String category, String sortBy) {
        java.util.stream.Stream<AuctionItem> stream = (base == null ? java.util.List.<AuctionItem>of() : base).stream();

        String q = query == null ? "" : query.trim().toLowerCase();
        if (!q.isBlank()) {
            stream = stream.filter(a -> containsIgnoreCase(a.getTitle(), q)
                    || containsIgnoreCase(a.getDescription(), q)
                    || containsIgnoreCase(a.getCategory(), q));
        }

        String c = category == null ? "" : category.trim();
        if (!c.isBlank()) {
            stream = stream.filter(a -> c.equalsIgnoreCase(a.getCategory()));
        }

        java.util.List<AuctionItem> filtered = stream.toList();
        if ("price_asc".equals(sortBy)) {
            return filtered.stream()
                    .sorted(java.util.Comparator.comparingLong(AuctionItem::getCurrentBidCents))
                    .toList();
        }
        if ("price_desc".equals(sortBy)) {
            return filtered.stream()
                    .sorted(java.util.Comparator.comparingLong(AuctionItem::getCurrentBidCents).reversed())
                    .toList();
        }
        return filtered.stream()
                .sorted((a, b) -> b.getEndTime().compareTo(a.getEndTime()))
                .toList();
    }

    private static boolean containsIgnoreCase(String value, String needleLower) {
        return value != null && value.toLowerCase().contains(needleLower);
    }

    private String mapSortToServer(String sortLabel) {
        if ("Price: Low → High".equals(sortLabel)) return "price_asc";
        if ("Price: High → Low".equals(sortLabel)) return "price_desc";
        return "newest";
    }

    private void renderLoadingState() {
        Platform.runLater(() -> {
            if (auctionFlow != null) {
                auctionFlow.getChildren().clear();
                VBox loading = new VBox();
                loading.getStyleClass().add("metric-card");
                Label t = new Label("Loading auctions...");
                t.getStyleClass().add("metric-label");
                loading.getChildren().add(t);
                auctionFlow.getChildren().add(loading);
            }
            if (auctionCountLabel != null) {
                auctionCountLabel.setText("Loading...");
            }
        });
    }

    private void renderAuctions(java.util.List<AuctionItem> items) {
        if (auctionFlow == null) {
            return;
        }
        auctionFlow.getChildren().clear();
        if (items == null || items.isEmpty()) {
            VBox empty = new VBox();
            empty.getStyleClass().add("metric-card");
            Label t = new Label("No auctions found");
            t.getStyleClass().add("metric-label");
            Label s = new Label("Try a different category or connect to a live server.");
            s.getStyleClass().add("section-copy");
            s.setWrapText(true);
            empty.getChildren().addAll(t, s);
            auctionFlow.getChildren().add(empty);
            if (auctionCountLabel != null) auctionCountLabel.setText("0 auctions");
            return;
        }

        for (AuctionItem item : items) {
            VBox card = createCard(item);
            auctionFlow.getChildren().add(card);
        }
        if (auctionCountLabel != null) {
            auctionCountLabel.setText(items.size() + (items.size() == 1 ? " auction" : " auctions"));
        }
    }

    private VBox createCard(AuctionItem item) {
        VBox card = new VBox();
        card.getStyleClass().addAll("metric-card");
        card.setPrefWidth(240);

        Label title = new Label(item.getTitle());
        title.getStyleClass().add("metric-label");

        Label price = new Label(String.format("%s", com.auction.shared.Constants.formatCents(item.getCurrentBidCents())));
        price.getStyleClass().add("section-copy");
        ImageView thumbView = new ImageView();
        thumbView.setFitWidth(220);
        thumbView.setFitHeight(140);
        thumbView.setPreserveRatio(true);
        thumbView.getStyleClass().add("image-placeholder");

        loadThumbnailAsync(item.getId(), 0, thumbView);

        Button view = new Button("View");
        view.getStyleClass().addAll("primary-button");
        view.setOnAction(evt -> {
            openAuctionDetail(item, 0);
        });

        // small thumbnail rail (up to 3 thumbnails)
        javafx.scene.layout.HBox rail = new javafx.scene.layout.HBox(6);
        for (int i = 0; i < 3; i++) {
            ImageView small = new ImageView();
            small.setFitWidth(64);
            small.setFitHeight(48);
            small.setPreserveRatio(true);
            small.getStyleClass().add("image-thumb");
            final int idx = i;
            small.setOnMouseClicked(e -> {
                openAuctionDetail(item, idx);
            });
            // prefetch hero on hover for snappier detail view
            small.setOnMouseEntered(e -> loadThumbnailToCache(item.getId(), 0));
            loadThumbnailAsync(item.getId(), i, small);
            rail.getChildren().add(small);
        }

        card.getChildren().addAll(thumbView, rail, title, price, view);
        return card;
    }

    private void openAuctionDetail(AuctionItem item, int heroIndex) {
        try {
            ClientContext context = ClientContext.getInstance();
            context.setCurrentAuctionId(item.getId());
            context.setPreviousViewName("gallery.fxml");
            Object ctrl = context.getViewLoader().loadView("auction_detail.fxml");
            if (ctrl instanceof AuctionDetailController) {
                AuctionDetailController detailController = (AuctionDetailController) ctrl;
                if (heroIndex >= 0) {
                    Platform.runLater(() -> detailController.showHeroImageIndex(heroIndex));
                }
            } else {
                showNavigationError("Auction detail controller was not available.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showNavigationError("Unable to open auction detail: " + ex.getMessage());
        }
    }

    private void showNavigationError(String message) {
        Platform.runLater(() -> {
            if (auctionCountLabel != null) {
                auctionCountLabel.setText(message);
            }
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Could not open auction detail");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void loadThumbnailAsync(int auctionId, int index, ImageView target) {
        String key = auctionId + ":" + index;
        Image cached = thumbnailCache.get(key);
        if (cached != null) {
            target.setImage(cached);
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                var service = ClientContext.getInstance().getRmiProvider().getService();
                byte[] bytes = service.getThumbnail(auctionId, index);
                if (bytes == null || bytes.length == 0) return null;
                return new Image(new ByteArrayInputStream(bytes));
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }, ThumbnailExecutor.getExecutor()).thenAccept(image -> {
            if (image != null) {
                thumbnailCache.put(key, image);
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

    private void loadThumbnailToCache(int auctionId, int index) {
        String key = auctionId + ":" + index;
        if (thumbnailCache.containsKey(key)) return;
        CompletableFuture.supplyAsync(() -> {
            try {
                var service = ClientContext.getInstance().getRmiProvider().getService();
                byte[] bytes = service.getThumbnail(auctionId, index);
                if (bytes == null || bytes.length == 0) return null;
                return new Image(new ByteArrayInputStream(bytes));
            } catch (Exception e) {
                return null;
            }
        }, ThumbnailExecutor.getExecutor()).thenAccept(image -> {
            if (image != null) thumbnailCache.put(key, image);
        });
    }

    private static Image loadPlaceholderImage() {
        InputStream stream = GalleryController.class.getResourceAsStream("/images/placeholder.png");
        if (stream == null) {
            return null;
        }
        return new Image(stream);
    }

    @FXML
    private void handleBackToDashboard() {
        try {
            // stop polling before leaving the view
            stopPolling();

            ClientContext context = ClientContext.getInstance();
            String targetView = context.getPreviousViewName();
            if (targetView == null || targetView.isBlank()) {
                targetView = "user_dashboard.fxml";
            }
            context.getViewLoader().loadView(targetView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLogout(javafx.event.ActionEvent event) {
        try {
            // stop background polling first
            stopPolling();

            ClientContext context = ClientContext.getInstance();
            try {
                var svc = context.getRmiProvider().getService();
                svc.logout(context.getSessionToken());
            } catch (Exception e) {
                // best-effort logout; log but continue to clear session locally
                e.printStackTrace();
            }
            context.clearSession();
            context.getViewLoader().loadView("login.fxml");
        } catch (IOException e) {
            e.printStackTrace();
            if (auctionCountLabel != null) auctionCountLabel.setText("Logout failed: " + e.getMessage());
        }
    }

    private void startPolling() {
        try {
            if (pollExecutor == null || pollExecutor.isShutdown()) {
                pollExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "gallery-poller");
                    t.setDaemon(true);
                    return t;
                });
            }
            // schedule with fixed rate; first run after interval to allow initial load
            pollTask = pollExecutor.scheduleAtFixedRate(() -> {
                try {
                    fetchAndRenderAuctionsAsync();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }, POLL_INTERVAL_SECONDS, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopPolling() {
        try {
            if (pollTask != null && !pollTask.isCancelled()) {
                pollTask.cancel(false);
            }
            if (pollExecutor != null && !pollExecutor.isShutdown()) {
                pollExecutor.shutdownNow();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            pollTask = null;
            pollExecutor = null;
        }
    }
}
