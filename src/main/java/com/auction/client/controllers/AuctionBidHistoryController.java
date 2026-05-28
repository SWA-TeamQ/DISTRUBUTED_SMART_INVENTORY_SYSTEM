package com.auction.client.controllers;

import com.auction.client.core.ClientContext;
import com.auction.client.service.BidHistoryService;
import com.auction.shared.models.Bid;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class AuctionBidHistoryController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label auctionIdLabel;
    @FXML private Button backButton;
    @FXML private TableView<Bid> bidHistoryTable;
    @FXML private TableColumn<Bid, String> timeColumn;
    @FXML private TableColumn<Bid, String> userColumn;
    @FXML private TableColumn<Bid, Long> amountColumn;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        if (timeColumn != null) {
            timeColumn.setCellValueFactory(new PropertyValueFactory<>("timestampFormatted"));
        }
        if (userColumn != null) {
            userColumn.setCellValueFactory(new PropertyValueFactory<>("bidderUsername"));
        }
        if (amountColumn != null) {
            amountColumn.setCellValueFactory(new PropertyValueFactory<>("amountCents"));
            amountColumn.setCellFactory(col -> new javafx.scene.control.TableCell<Bid, Long>() {
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

        int auctionId = ClientContext.getInstance().getCurrentAuctionId();
        if (auctionId >= 0) {
            if (auctionIdLabel != null) {
                auctionIdLabel.setText("Auction #" + auctionId);
            }
            loadHistory(auctionId);
        } else {
            if (statusLabel != null) statusLabel.setText("No auction selected.");
        }
    }

    private void loadHistory(int auctionId) {
        if (bidHistoryTable != null) {
            bidHistoryTable.getItems().clear();
        }
        Platform.runLater(() -> {
            if (statusLabel != null) statusLabel.setText("Loading bid history...");
        });

        BidHistoryService.loadBidHistoryAsync(auctionId).whenComplete((list, throwable) -> {
            if (throwable != null) {
                Platform.runLater(() -> {
                    if (statusLabel != null) statusLabel.setText("Failed to load bid history.");
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Bid History Error");
                    alert.setHeaderText("Could not load bid history");
                    alert.setContentText(throwable.getCause() == null ? throwable.getMessage() : throwable.getCause().getMessage());
                    alert.showAndWait();
                });
                return;
            }

            Platform.runLater(() -> {
                if (bidHistoryTable != null) {
                    bidHistoryTable.getItems().setAll(list == null ? java.util.List.of() : list);
                }
                if (statusLabel != null) {
                    int size = list == null ? 0 : list.size();
                    statusLabel.setText(size + (size == 1 ? " bid loaded" : " bids loaded"));
                }
            });
        });
    }

    @FXML
    private void handleRefreshHistory() {
        int auctionId = ClientContext.getInstance().getCurrentAuctionId();
        if (auctionId >= 0) {
            loadHistory(auctionId);
        }
    }

    @FXML
    private void handleBackToDetail() {
        try {
            ClientContext.getInstance().getViewLoader().loadView("auction_detail.fxml");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
