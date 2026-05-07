package com.auction.server.repository;

import com.auction.shared.models.Bid;

import java.sql.Connection;
import java.util.List;

public class BidRepository {
    private final Connection connection;

    public BidRepository(Connection connection) {
        this.connection = connection;
    }

    public void insertBid(Bid bid) {
        String sql = "INSERT INTO bids (auction_item_id, bidder_username, amount, timestamp) VALUES (?, ?, ?, ?)";
        try (var pstmt = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, bid.getAuctionItemId());
            pstmt.setString(2, bid.getBidderUsername());
            pstmt.setDouble(3, bid.getAmount());
            pstmt.setString(4, bid.getTimestamp());
            pstmt.executeUpdate();
            try (var rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) bid.setId(rs.getInt(1));
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to insert bid", e);
        }
    }

    public List<Bid> findBidsByAuctionId(int auctionId) {
        List<Bid> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM bids WHERE auction_item_id = ? ORDER BY timestamp DESC";
        try (var pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, auctionId);
            try (var rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Bid b = new Bid();
                    b.setId(rs.getInt("id"));
                    b.setAuctionItemId(rs.getInt("auction_item_id"));
                    b.setBidderUsername(rs.getString("bidder_username"));
                    b.setAmount(rs.getDouble("amount"));
                    b.setTimestamp(rs.getString("timestamp"));
                    list.add(b);
                }
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to fetch bids", e);
        }
        return list;
    }

    public int countBidsByAuctionId(int auctionId) {
        String sql = "SELECT COUNT(*) FROM bids WHERE auction_item_id = ?";
        try (var pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, auctionId);
            try (var rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to count bids", e);
        }
        return 0;
    }
}
