package com.auction.client.controllers;

import javafx.fxml.FXML;

public class SellerDashboardController {

    @FXML private javafx.scene.control.TableView<com.auction.shared.models.AuctionItem> auctionsTable;
    @FXML private javafx.scene.control.TextField titleField;
    @FXML private javafx.scene.control.TextArea descArea;
    @FXML private javafx.scene.control.TextField categoryField;
    @FXML private javafx.scene.control.TextField priceField;
    @FXML private javafx.scene.control.TextField endTimeField;
    @FXML private javafx.scene.control.Label imagesLabel;
    @FXML private javafx.scene.control.Label statusLabel;

    private byte[] img1Bytes, img2Bytes, img3Bytes;

    @FXML
    public void initialize() {
        loadAuctions();
    }

    private void loadAuctions() {
        try {
            com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
            com.auction.shared.interfaces.IAuctionService service = context.getRmiProvider().getService();
            // Since there's no getMySellerAuctions, we get all active and filter by sellerUsername
            // Wait, there might be a getAuctionById, getActiveAuctions, getMyWonAuctions.
            // Oh, the interface IAuctionService has no "getMyAuctions(String token)".
            // Let's use getActiveAuctions and filter locally, or just rely on the table.
            java.util.List<com.auction.shared.models.AuctionItem> all = service.getActiveAuctions();
            java.util.List<com.auction.shared.models.AuctionItem> mine = all.stream()
                .filter(a -> a.getSellerUsername().equals(context.getUsername()))
                .toList();
            auctionsTable.getItems().setAll(mine);
        } catch (Exception e) {
            statusLabel.setText("Failed to load auctions: " + e.getMessage());
        }
    }

    @FXML
    private void handleCreateAuction() {
        try {
            long cents = (long) (Double.parseDouble(priceField.getText()) * 100);
            int minutes = Integer.parseInt(endTimeField.getText());
            java.time.Instant end = java.time.Instant.now().plus(java.time.Duration.ofMinutes(minutes));
            
            com.auction.shared.models.AuctionItem item = new com.auction.shared.models.AuctionItem(
                0, titleField.getText(), descArea.getText(), categoryField.getText(),
                cents, com.auction.client.core.ClientContext.getInstance().getUsername(),
                java.time.Instant.now().toString(), end.toString(), null
            );
            
            com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
            int id = context.getRmiProvider().getService().createAuction(item, img1Bytes, img2Bytes, img3Bytes, context.getSessionToken());
            statusLabel.setText("Created auction #" + id);
            loadAuctions();
            
            titleField.clear(); descArea.clear(); categoryField.clear(); priceField.clear(); endTimeField.clear();
            img1Bytes = img2Bytes = img3Bytes = null;
            imagesLabel.setText("No images selected");
        } catch (Exception e) {
            statusLabel.setText("Error creating: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancelAuction() {
        com.auction.shared.models.AuctionItem selected = auctionsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
                context.getRmiProvider().getService().cancelAuction(selected.getId(), context.getSessionToken());
                loadAuctions();
            } catch (Exception e) {
                statusLabel.setText("Cancel failed: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleRelistAuction() {
        com.auction.shared.models.AuctionItem selected = auctionsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                java.time.Instant newEnd = java.time.Instant.now().plus(java.time.Duration.ofDays(1));
                com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
                context.getRmiProvider().getService().relistAuction(selected.getId(), newEnd.toString(), context.getSessionToken());
                loadAuctions();
            } catch (Exception e) {
                statusLabel.setText("Relist failed: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleExportCSV() {
        try {
            com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
            byte[] csv = context.getRmiProvider().getService().exportAuctionsToCSV(context.getSessionToken());
            java.io.File file = new java.io.File("my_auctions_export.csv");
            java.nio.file.Files.write(file.toPath(), csv);
            statusLabel.setText("Exported to " + file.getAbsolutePath());
        } catch (Exception e) {
            statusLabel.setText("Export failed: " + e.getMessage());
        }
    }

    @FXML
    private void handlePickImg1() { img1Bytes = pickImage(); updateImagesLabel(); }
    @FXML
    private void handlePickImg2() { img2Bytes = pickImage(); updateImagesLabel(); }
    @FXML
    private void handlePickImg3() { img3Bytes = pickImage(); updateImagesLabel(); }

    private byte[] pickImage() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Images", "*.jpg", "*.png"));
        java.io.File f = fc.showOpenDialog(auctionsTable.getScene().getWindow());
        if (f != null) {
            try { return java.nio.file.Files.readAllBytes(f.toPath()); }
            catch (Exception e) { statusLabel.setText("Read failed"); }
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
            com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
            context.getRmiProvider().getService().logout(context.getSessionToken());
            context.clearSession();
            context.getViewLoader().loadView("login.fxml");
        } catch (Exception e) {
            statusLabel.setText("Logout failed: " + e.getMessage());
        }
    }
}
