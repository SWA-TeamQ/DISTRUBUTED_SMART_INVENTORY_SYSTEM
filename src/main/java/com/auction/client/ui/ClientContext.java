package com.auction.client.ui;

import com.auction.client.service.MockAuctionService;
import com.auction.shared.interfaces.IAuctionService;
import com.auction.shared.models.AuctionItem;

/**
 * Shared client state for the mock auction flow.
 */
public final class ClientContext {

    private static IAuctionService auctionService = new MockAuctionService();
    private static AuctionItem selectedAuction;

    private ClientContext() {}

    public static IAuctionService getAuctionService() {
        return auctionService;
    }

    public static void setAuctionService(IAuctionService service) {
        auctionService = service == null ? new MockAuctionService() : service;
    }

    public static AuctionItem getSelectedAuction() {
        return selectedAuction;
    }

    public static void setSelectedAuction(AuctionItem auctionItem) {
        selectedAuction = auctionItem;
    }
}