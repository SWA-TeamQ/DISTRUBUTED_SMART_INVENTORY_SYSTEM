package com.auction.client.controllers;

import com.auction.client.service.MockAuctionService;
import com.auction.client.ui.ClientContext;
import com.auction.client.ui.ClientNavigator;
import com.auction.shared.interfaces.IAuctionService;
import com.auction.shared.models.AuctionItem;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.MouseEvent;

import java.util.List;

public class GalleryController {

    @FXML private ListView<AuctionItem> auctionListView;
    @FXML private Label statusLabel;

    private final IAuctionService service = ClientContext.getAuctionService();

    @FXML
    public void initialize() {
        if (service == null) {
            ClientContext.setAuctionService(new MockAuctionService());
        }
        auctionListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        auctionListView.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(AuctionItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null
                        ? null
                        : "#" + item.getId() + " " + item.getTitle() + " - " + item.getStatus()
                        + " ($" + String.format("%.2f", item.getCurrentBidCents() / 100.0) + ")");
            }
        });
        loadAuctions();
    }

    private void loadAuctions() {
        try {
            List<AuctionItem> auctions = service.getActiveAuctions();
            auctionListView.getItems().setAll(auctions);
            if (!auctions.isEmpty()) {
                auctionListView.getSelectionModel().selectFirst();
                ClientContext.setSelectedAuction(auctions.get(0));
            }
            if (statusLabel != null) {
                statusLabel.setText("Loaded " + auctions.size() + " mock auctions.");
            }
        } catch (Exception ex) {
            if (statusLabel != null) {
                statusLabel.setText("Failed to load auctions: " + ex.getMessage());
            }
        }
    }

    @FXML
    private void openSelectedAuction() {
        AuctionItem selected = auctionListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            if (statusLabel != null) {
                statusLabel.setText("Select an auction first.");
            }
            return;
        }
        ClientContext.setSelectedAuction(selected);
        try {
            ClientNavigator.loadView("auction_detail.fxml");
        } catch (Exception ex) {
            if (statusLabel != null) {
                statusLabel.setText("Failed to open detail: " + ex.getMessage());
            }
        }
    }

    @FXML
    private void handleDoubleClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            openSelectedAuction();
        }
    }
}
