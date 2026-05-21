package com.auction.client.controllers;

import com.auction.client.service.PollingService;
import com.auction.client.ui.ClientContext;
import com.auction.client.ui.ClientNavigator;
import com.auction.shared.interfaces.IAuctionService;
import com.auction.shared.models.AuctionItem;
import com.auction.shared.models.Bid;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.util.List;

public class AuctionDetailController {

    @FXML private Label titleLabel;
    @FXML private Label statusLabel;
    @FXML private Label currentBidLabel;
    @FXML private Label bidderLabel;
    @FXML private TextField bidField;
    @FXML private Label messageLabel;
    @FXML private ListView<Bid> bidHistoryView;
    @FXML private Button bidButton;

    private final IAuctionService service = ClientContext.getAuctionService();
    private final ObservableList<Bid> bidHistory = FXCollections.observableArrayList();
    private PollingService pollingService;
    private AuctionItem currentAuction;

    @FXML
    public void initialize() {
        bidHistoryView.setItems(bidHistory);
        currentAuction = ClientContext.getSelectedAuction();
        if (currentAuction == null) {
            loadFallbackAuction();
        } else {
            refreshView(currentAuction);
        }
        pollingService = new PollingService(service);
        if (currentAuction != null) {
            pollingService.startPolling(currentAuction.getId(), item -> Platform.runLater(() -> refreshView(item)));
        }
    }
    
    public void shutdown() {
        if (pollingService != null) {
            pollingService.shutdown();
        }
    }

    @FXML
    private void handleBid() {
        if (currentAuction == null) {
            return;
        }
        try {
            long amountCents = Math.round(Double.parseDouble(bidField.getText().trim()) * 100.0);
            service.placeBid(currentAuction.getId(), amountCents, currentAuction.getCurrentBidCents(), "mock-bidder");
            showMessage("Bid placed successfully.");
            refreshAuctionData();
        } catch (Exception ex) {
            showMessage("Bid failed: " + ex.getMessage());
        }
    }

    @FXML
    private void goBackToGallery() {
        shutdown();
        try {
            ClientNavigator.loadView("gallery.fxml");
        } catch (Exception ex) {
            showMessage("Unable to go back: " + ex.getMessage());
        }
    }

    private void refreshView(AuctionItem item) {
        currentAuction = item;
        titleLabel.setText(item.getTitle());
        statusLabel.setText("Status: " + item.getStatus());
        currentBidLabel.setText("Current Bid: $" + String.format("%.2f", item.getCurrentBidCents() / 100.0));
        bidderLabel.setText("Highest Bidder: " + (item.getHighestBidderUsername() == null ? "None" : item.getHighestBidderUsername()));
        bidButton.setDisable(!"ACTIVE".equals(item.getStatus()));
        refreshAuctionData();
    }

    private void refreshAuctionData() {
        try {
            AuctionItem item = service.getAuctionById(currentAuction.getId());
            currentAuction = item;
            titleLabel.setText(item.getTitle());
            statusLabel.setText("Status: " + item.getStatus());
            currentBidLabel.setText("Current Bid: $" + String.format("%.2f", item.getCurrentBidCents() / 100.0));
            bidderLabel.setText("Highest Bidder: " + (item.getHighestBidderUsername() == null ? "None" : item.getHighestBidderUsername()));
            bidButton.setDisable(!"ACTIVE".equals(item.getStatus()));
            List<Bid> bids = service.getBidHistory(item.getId());
            bidHistory.setAll(bids);
        } catch (Exception ex) {
            showMessage("Refresh failed: " + ex.getMessage());
        }
    }

    private void loadFallbackAuction() {
        try {
            List<AuctionItem> auctions = service.getActiveAuctions();
            if (!auctions.isEmpty()) {
                currentAuction = auctions.get(0);
                ClientContext.setSelectedAuction(currentAuction);
                refreshView(currentAuction);
            }
        } catch (Exception ex) {
            showMessage("No auction data available: " + ex.getMessage());
        }
    }

    private void showMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
        }
    }
}
