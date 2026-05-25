package com.auction.client.controllers;

import com.auction.client.core.ClientContext;
import com.auction.shared.models.AuctionItem;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
import com.auction.client.service.ThumbnailExecutor;

public class GalleryController {

    @FXML private FlowPane auctionFlow;
    @FXML private javafx.scene.control.TextField searchField;
    @FXML private Label auctionCountLabel;

    private final Map<String, Image> thumbnailCache = new ConcurrentHashMap<>();
    private static final Image PLACEHOLDER_IMAGE = loadPlaceholderImage();
    private java.util.List<AuctionItem> allAuctions = java.util.List.of();

    @FXML
    public void initialize() {
        try {
            var context = ClientContext.getInstance();
            var service = context.getRmiProvider().getService();
            List<AuctionItem> items = service.getActiveAuctions();
            allAuctions = (items == null) ? java.util.List.of() : items;
            Platform.runLater(() -> renderAuctions(allAuctions));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearch() {
        String q = searchField == null ? "" : searchField.getText();
        if (q == null || q.isBlank()) {
            renderAuctions(allAuctions);
            return;
        }

        String needle = q.trim().toLowerCase();
        java.util.List<AuctionItem> filtered = allAuctions.stream()
            .filter(a -> containsIgnoreCase(a.getTitle(), needle)
                || containsIgnoreCase(a.getDescription(), needle)
                || containsIgnoreCase(a.getCategory(), needle))
            .toList();
        renderAuctions(filtered);
    }

    private boolean containsIgnoreCase(String value, String needleLower) {
        return value != null && value.toLowerCase().contains(needleLower);
    }

    private void renderAuctions(java.util.List<AuctionItem> items) {
        auctionFlow.getChildren().clear();
        if (items == null || items.isEmpty()) {
            VBox empty = new VBox();
            empty.getStyleClass().add("metric-card");
            Label t = new Label("No auctions found");
            t.getStyleClass().add("metric-label");
            Label s = new Label("Try a different search term or connect to a live server.");
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
            try {
                ClientContext context = ClientContext.getInstance();
                context.setPreviousViewName("gallery.fxml");
                Object ctrl = context.getViewLoader().loadView("auction_detail.fxml");
                if (ctrl instanceof AuctionDetailController) {
                    ((AuctionDetailController) ctrl).loadAuction(item.getId());
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
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
                try {
                    ClientContext context = ClientContext.getInstance();
                    context.setPreviousViewName("gallery.fxml");
                    Object ctrl = context.getViewLoader().loadView("auction_detail.fxml");
                    if (ctrl instanceof AuctionDetailController) {
                        ((AuctionDetailController) ctrl).loadAuction(item.getId());
                        ((AuctionDetailController) ctrl).showHeroImageIndex(idx);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
            // prefetch hero on hover for snappier detail view
            small.setOnMouseEntered(e -> loadThumbnailToCache(item.getId(), 0));
            loadThumbnailAsync(item.getId(), i, small);
            rail.getChildren().add(small);
        }

        card.getChildren().addAll(thumbView, rail, title, price, view);
        return card;
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
            throw new IllegalStateException("Missing resource: /images/placeholder.png");
        }
        return new Image(stream);
    }

    @FXML
    private void handleBackToDashboard() {
        try {
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
}
