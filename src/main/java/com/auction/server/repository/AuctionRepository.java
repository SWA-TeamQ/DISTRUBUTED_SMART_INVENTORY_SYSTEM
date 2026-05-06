package com.auction.server.repository;

import com.auction.shared.models.AuctionItem;

import java.sql.Connection;
import java.util.List;

public class AuctionRepository {
    private final Connection connection;

    public AuctionRepository(Connection connection) {
        this.connection = connection;
    }

    public int insertAuction(AuctionItem item) { return 0; }
    public AuctionItem findAuctionById(int id) { return null; }
    public List<AuctionItem> findActiveAuctions() { return List.of(); }
    public List<AuctionItem> findAuctionsBySeller(String sellerUsername) { return List.of(); }
    public void updateAuctionBid(int auctionId, double newBid, String bidderUsername) {}
    public void updateAuctionStatus(int auctionId, String status) {}
    public void updateAuctionEndTime(int auctionId, String newEndTime) {}
}
