package com.auction.client.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import com.auction.client.core.ClientContext;
import com.auction.shared.models.AuctionItem;

public class AuctionDetailController {

    @FXML private VBox bidderControls;
    @FXML private VBox sellerControls;
    @FXML private Label controlsTitleLabel;
    @FXML private Label controlsDescLabel;

    @FXML
    public void initialize() {
        int auctionId = ClientContext.getInstance().getCurrentAuctionId();
        if (auctionId != -1) {
            try {
                AuctionItem auction = ClientContext.getInstance().getRmiProvider().getService().getAuctionById(auctionId);
                if (auction != null) {
                    String currentUsername = ClientContext.getInstance().getUsername();
                    if (currentUsername.equals(auction.getSellerUsername())) {
                        bidderControls.setVisible(false);
                        bidderControls.setManaged(false);
                        sellerControls.setVisible(true);
                        sellerControls.setManaged(true);
                        if (controlsTitleLabel != null) controlsTitleLabel.setText("Management Settings");
                        if (controlsDescLabel != null) controlsDescLabel.setText("As the seller, you can manage this auction here.");
                    } else {
                        sellerControls.setVisible(false);
                        sellerControls.setManaged(false);
                        bidderControls.setVisible(true);
                        bidderControls.setManaged(true);
                        if (controlsTitleLabel != null) controlsTitleLabel.setText("Bid Controls");
                        if (controlsDescLabel != null) controlsDescLabel.setText("Place your bids and monitor status here.");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // TODO: setup polling for updates, handle bid placement
    }
    
    public void shutdown() {
        // TODO: stop polling thread
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
            com.auction.client.core.ClientContext.getInstance().getViewLoader().loadView("gallery.fxml");
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
}
