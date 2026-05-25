package com.auction.client.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class UserDashboardController {

  @FXML
  private javafx.scene.control.TableView<
    com.auction.shared.models.AuctionItem
  > marketTable;

  @FXML
  private javafx.scene.control.TableView<
    com.auction.shared.models.AuctionItem
  > myListingsTable;

  @FXML
  private javafx.scene.control.TableView<
    com.auction.shared.models.Bid
  > myBidsTable;

  @FXML
  private javafx.scene.control.TableView<
    com.auction.shared.models.AuctionItem
  > wonAuctionsTable;

  @FXML
  private javafx.scene.control.TextField titleField;

  @FXML
  private javafx.scene.control.TextArea descArea;

  @FXML
  private javafx.scene.control.TextField categoryField;

  @FXML
  private javafx.scene.control.TextField priceField;

  @FXML
  private javafx.scene.control.TextField endTimeField;

  @FXML
  private javafx.scene.control.Label imagesLabel;

  @FXML
  private javafx.scene.control.Label statusLabel;

  @FXML
  private Label marketCountLabel;

  @FXML
  private Label listingsCountLabel;

  @FXML
  private Label bidsCountLabel;

  @FXML
  private Label winsCountLabel;

  private byte[] img1Bytes, img2Bytes, img3Bytes;

  @FXML
  public void initialize() {
    refreshDashboard();
  }

  private void refreshDashboard() {
    try {
      com.auction.client.core.ClientContext context =
        com.auction.client.core.ClientContext.getInstance();
      com.auction.shared.interfaces.IAuctionService service = context
        .getRmiProvider()
        .getService();
      java.util.List<com.auction.shared.models.AuctionItem> activeAuctions =
        service.getActiveAuctions();
      java.util.List<com.auction.shared.models.AuctionItem> mine =
        service.getActiveAuctionsBySeller(
          context.getUsername(),
          context.getSessionToken()
        );
      java.util.List<com.auction.shared.models.Bid> bids = service.getMyBids(
        context.getSessionToken()
      );
      java.util.List<com.auction.shared.models.AuctionItem> won =
        service.getMyWonAuctions(context.getSessionToken());

      marketTable.getItems().setAll(activeAuctions);
      myListingsTable.getItems().setAll(mine);
      myBidsTable.getItems().setAll(bids);
      wonAuctionsTable.getItems().setAll(won);

      if (marketCountLabel != null) marketCountLabel.setText(
        String.valueOf(activeAuctions.size())
      );
      if (listingsCountLabel != null) listingsCountLabel.setText(
        String.valueOf(mine.size())
      );
      if (bidsCountLabel != null) bidsCountLabel.setText(
        String.valueOf(bids.size())
      );
      if (winsCountLabel != null) winsCountLabel.setText(
        String.valueOf(won.size())
      );
      statusLabel.setText("Dashboard refreshed successfully.");
    } catch (Exception e) {
      statusLabel.setText("Failed to load dashboard: " + e.getMessage());
    }
  }

  @FXML
  private void handleRefreshDashboard() {
    refreshDashboard();
  }

  @FXML
  private void handleOpenGallery() {
    try {
      com.auction.client.core.ClientContext context =
        com.auction.client.core.ClientContext.getInstance();
      context.setPreviousViewName("user_dashboard.fxml");
      context.getViewLoader().loadView("gallery.fxml");
    } catch (java.io.IOException e) {
      throw new RuntimeException(e);
    }
  }

  @FXML
  private void handleOpenAuctionDetail() {
    com.auction.shared.models.AuctionItem selected = null;
    
    if (marketTable.getSelectionModel().getSelectedItem() != null) {
        selected = marketTable.getSelectionModel().getSelectedItem();
    } else if (myListingsTable.getSelectionModel().getSelectedItem() != null) {
        selected = myListingsTable.getSelectionModel().getSelectedItem();
    } else if (wonAuctionsTable.getSelectionModel().getSelectedItem() != null) {
        selected = wonAuctionsTable.getSelectionModel().getSelectedItem();
    }

    if (selected != null) {
        try {
          com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
          context.setCurrentAuctionId(selected.getId());
          context.setPreviousViewName("user_dashboard.fxml");
          context.getViewLoader().loadView("auction_detail.fxml");
        } catch (java.io.IOException e) {
          throw new RuntimeException(e);
        }
    } else {
        if (statusLabel != null) statusLabel.setText("Please select an auction first.");
    }
  }

  @FXML
  private void handleCreateAuction() {
    try {
      long cents = (long) (Double.parseDouble(priceField.getText()) * 100);
      int minutes = Integer.parseInt(endTimeField.getText());
      java.time.Instant end = java.time.Instant.now().plus(
        java.time.Duration.ofMinutes(minutes)
      );

      com.auction.shared.models.AuctionItem item =
        new com.auction.shared.models.AuctionItem(
          0,
          titleField.getText(),
          descArea.getText(),
          categoryField.getText(),
          cents,
          com.auction.client.core.ClientContext.getInstance().getUsername(),
          java.time.Instant.now().toString(),
          end.toString(),
          null
        );

      com.auction.client.core.ClientContext context =
        com.auction.client.core.ClientContext.getInstance();
      int id = context
        .getRmiProvider()
        .getService()
        .createAuction(
          item,
          img1Bytes,
          img2Bytes,
          img3Bytes,
          context.getSessionToken()
        );
      statusLabel.setText("Created auction #" + id);
      refreshDashboard();

      titleField.clear();
      descArea.clear();
      categoryField.clear();
      priceField.clear();
      endTimeField.clear();
      img1Bytes = img2Bytes = img3Bytes = null;
      imagesLabel.setText("No images selected");
    } catch (Exception e) {
      statusLabel.setText("Error creating: " + e.getMessage());
    }
  }

  @FXML
  private void handleCancelAuction() {
    com.auction.shared.models.AuctionItem selected = myListingsTable
      .getSelectionModel()
      .getSelectedItem();
    if (selected != null) {
      try {
        com.auction.client.core.ClientContext context =
          com.auction.client.core.ClientContext.getInstance();
        context
          .getRmiProvider()
          .getService()
          .cancelAuction(selected.getId(), context.getSessionToken());
        refreshDashboard();
      } catch (Exception e) {
        statusLabel.setText("Cancel failed: " + e.getMessage());
      }
    }
  }

  @FXML
  private void handleRelistAuction() {
    com.auction.shared.models.AuctionItem selected = myListingsTable
      .getSelectionModel()
      .getSelectedItem();
    if (selected != null) {
      try {
        java.time.Instant newEnd = java.time.Instant.now().plus(
          java.time.Duration.ofDays(1)
        );
        com.auction.client.core.ClientContext context =
          com.auction.client.core.ClientContext.getInstance();
        context
          .getRmiProvider()
          .getService()
          .relistAuction(
            selected.getId(),
            newEnd.toString(),
            context.getSessionToken()
          );
        refreshDashboard();
      } catch (Exception e) {
        statusLabel.setText("Relist failed: " + e.getMessage());
      }
    }
  }

  @FXML
  private void handleExportCSV() {
    try {
      com.auction.client.core.ClientContext context =
        com.auction.client.core.ClientContext.getInstance();
      byte[] csv = context
        .getRmiProvider()
        .getService()
        .exportAuctionsToCSV(context.getSessionToken());
      java.io.File file = new java.io.File("my_auctions_export.csv");
      java.nio.file.Files.write(file.toPath(), csv);
      statusLabel.setText("Exported to " + file.getAbsolutePath());
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
    javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
    fc
      .getExtensionFilters()
      .add(
        new javafx.stage.FileChooser.ExtensionFilter("Images", "*.jpg", "*.png")
      );
    java.io.File f = fc.showOpenDialog(marketTable.getScene().getWindow());
    if (f != null) {
      try {
        return java.nio.file.Files.readAllBytes(f.toPath());
      } catch (Exception e) {
        statusLabel.setText("Read failed");
      }
    }
    return null;
  }

  private void updateImagesLabel() {
    int count = 0;
    if (img1Bytes != null) count++;
    if (img2Bytes != null) count++;
    if (img3Bytes != null) count++;
    imagesLabel.setText(count + " images selected");
  }

  @FXML
  private void handleLogout() {
    try {
      com.auction.client.core.ClientContext context =
        com.auction.client.core.ClientContext.getInstance();
      context.getRmiProvider().getService().logout(context.getSessionToken());
      context.clearSession();
      context.getViewLoader().loadView("login.fxml");
    } catch (Exception e) {
      statusLabel.setText("Logout failed: " + e.getMessage());
    }
  }
}
