package com.auction.server.repository;

import com.auction.shared.models.Bid;

import java.sql.Connection;
import java.util.List;

public class BidRepository {
    private final Connection connection;

    public BidRepository(Connection connection) {
        this.connection = connection;
    }

    public void insertBid(Bid bid) {}
    public List<Bid> findBidsByAuctionId(int auctionId) { return List.of(); }
    public int countBidsByAuctionId(int auctionId) { return 0; }
}
