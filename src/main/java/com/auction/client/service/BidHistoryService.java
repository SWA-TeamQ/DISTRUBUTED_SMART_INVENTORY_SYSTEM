package com.auction.client.service;

import com.auction.client.core.ClientContext;
import com.auction.shared.models.Bid;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class BidHistoryService {

    private BidHistoryService() {
    }

    public static CompletableFuture<List<Bid>> loadBidHistoryAsync(int auctionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return ClientContext.getInstance()
                    .getRmiProvider()
                    .getService()
                    .getBidHistory(auctionId);
            } catch (Exception e) {
                throw new java.util.concurrent.CompletionException(e);
            }
        });
    }
}