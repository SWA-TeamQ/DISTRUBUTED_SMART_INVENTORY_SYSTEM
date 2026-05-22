package com.auction.client.ui;

import com.auction.shared.models.AuctionItem;

/**
 * Shared client UI state.
 */
public final class ClientContext {

    private static AuctionItem selectedAuction;

    private ClientContext() {}

    public static AuctionItem getSelectedAuction() {
        return selectedAuction;
    }

    public static void setSelectedAuction(AuctionItem auctionItem) {
        selectedAuction = auctionItem;
    }
}