package com.auction.client.controllers;

import com.auction.client.core.ClientContext;
import com.auction.client.service.PollingService;
import com.auction.client.service.ThumbnailExecutor;
import com.auction.shared.Constants;
import com.auction.shared.models.AuctionItem;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class GalleryController {

  @FXML
  private FlowPane auctionFlow;

  @FXML
  private TextField searchField;

  @FXML
  private Label auctionCountLabel;

  private final Map<String, Image> cache = new ConcurrentHashMap<>();
  private static final Image PLACEHOLDER = loadPlaceholder();
  private List<AuctionItem> allAuctions = List.of();
  private PollingService pollingService;

  @FXML
  public void initialize() {
    try {
      var service = ClientContext.getInstance().getRmiProvider().getService();
      pollingService = new PollingService();
      pollingService.startPolling(
        () -> {
          try {
            List<AuctionItem> items = service.getActiveAuctions();
            allAuctions = (items == null) ? List.of() : items;
            Platform.runLater(this::handleSearch);
          } catch (Exception ignored) {}
        },
        2
      );
      render(allAuctions);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @FXML
  private void handleSearch() {
    String q = (searchField == null || searchField.getText() == null)
      ? ""
      : searchField.getText().toLowerCase();
    List<AuctionItem> filtered = allAuctions
      .stream()
      .filter(
        a ->
          a.getTitle().toLowerCase().contains(q) ||
          a.getCategory().toLowerCase().contains(q)
      )
      .toList();
    render(filtered);
  }

  private void render(List<AuctionItem> items) {
    auctionFlow.getChildren().clear();
    if (items.isEmpty()) {
      auctionFlow.getChildren().add(new Label("No auctions found."));
      return;
    }
    for (AuctionItem item : items)
      auctionFlow.getChildren().add(createCard(item));
    auctionCountLabel.setText(items.size() + " auction(s)");
  }

  private VBox createCard(AuctionItem item) {
    VBox card = new VBox(10);
    card.getStyleClass().add("metric-card");
    card.setPrefWidth(220);

    ImageView hero = createThumbView(220, 140);
    loadThumbAsync(item.getId(), 0, hero);

    HBox rail = new HBox(5);
    for (int i = 0; i < 3; i++) {
      ImageView iv = createThumbView(64, 48);
      loadThumbAsync(item.getId(), i, iv);
      int idx = i;
      iv.setOnMouseClicked(e -> loadDetail(item, idx));
      rail.getChildren().add(iv);
    }

    Button btn = new Button("View");
    btn.setOnAction(e -> loadDetail(item, 0));

    card
      .getChildren()
      .addAll(
        hero,
        rail,
        new Label(item.getTitle()),
        new Label(Constants.formatCents(item.getCurrentBidCents())),
        btn
      );
    return card;
  }

  private ImageView createThumbView(double w, double h) {
    ImageView iv = new ImageView();
    iv.setFitWidth(w);
    iv.setFitHeight(h);
    iv.setPreserveRatio(true);
    return iv;
  }

  private void loadThumbAsync(int id, int idx, ImageView iv) {
    String key = id + ":" + idx;
    if (cache.containsKey(key)) {
      iv.setImage(cache.get(key));
      return;
    }
    CompletableFuture.supplyAsync(
      () -> {
        try {
          byte[] b = ClientContext.getInstance()
            .getRmiProvider()
            .getService()
            .getThumbnail(id, idx);
          return (b == null || b.length == 0)
            ? null
            : new Image(new ByteArrayInputStream(b));
        } catch (Exception e) {
          return null;
        }
      },
      ThumbnailExecutor.getExecutor()
    ).thenAccept(img -> {
      Image fin = (img != null) ? img : PLACEHOLDER;
      cache.put(key, fin);
      Platform.runLater(() -> iv.setImage(fin));
    });
  }

  private void loadDetail(AuctionItem item, int idx) {
    if (pollingService != null) pollingService.shutdown();
    try {
      ClientContext ctx = ClientContext.getInstance();
      ctx.setPreviousViewName("gallery.fxml");
      var loader = (AuctionDetailController) ctx
        .getViewLoader()
        .loadView("auction_detail.fxml");
      loader.loadAuction(item.getId());
      loader.showHeroImageIndex(idx);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @FXML
  private void handleBackToDashboard() {
    if (pollingService != null) pollingService.shutdown();
    try {
      ClientContext.getInstance()
        .getViewLoader()
        .loadView("user_dashboard.fxml");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static Image loadPlaceholder() {
    InputStream s = GalleryController.class.getResourceAsStream(
      "/images/placeholder.png"
    );
    return (s == null) ? null : new Image(s);
  }
}
