package com.auction.client.state;

import com.auction.shared.models.AuctionItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Shared client-side auction state so the dashboard table and gallery render the same live data.
 */
public final class AuctionUiState {

  private static final AuctionUiState INSTANCE = new AuctionUiState();

  private final ObservableList<AuctionItem> activeAuctions =
    FXCollections.observableArrayList();

  private AuctionUiState() {}

  public static AuctionUiState getInstance() {
    return INSTANCE;
  }

  public ObservableList<AuctionItem> getActiveAuctions() {
    return activeAuctions;
  }
}
