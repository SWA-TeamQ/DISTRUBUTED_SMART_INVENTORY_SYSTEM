package com.auction.server.repository;

import com.auction.shared.models.AuctionItem;

import java.sql.Connection;
import java.util.List;

public class AuctionRepository {
    private final Connection connection;

    public AuctionRepository(Connection connection) {
        this.connection = connection;
    }

    public int insertAuction(AuctionItem item) {
        String sql = "INSERT INTO auction_items (title, description, category, starting_price, current_bid, " +
                "seller_username, start_time, end_time, status, img1, img2, img3) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (var pstmt = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, item.getTitle());
            pstmt.setString(2, item.getDescription());
            pstmt.setString(3, item.getCategory());
            pstmt.setDouble(4, item.getStartingPrice());
            pstmt.setDouble(5, item.getCurrentBid());
            pstmt.setString(6, item.getSellerUsername());
            pstmt.setString(7, item.getStartTime());
            pstmt.setString(8, item.getEndTime());
            pstmt.setString(9, item.getStatus());
            pstmt.setString(10, item.getImg1());
            pstmt.setString(11, item.getImg2());
            pstmt.setString(12, item.getImg3());
            pstmt.executeUpdate();
            try (var rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    item.setId(id);
                    return id;
                }
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to insert auction", e);
        }
        return -1;
    }

    private AuctionItem mapRowToAuction(java.sql.ResultSet rs) throws java.sql.SQLException {
        AuctionItem item = new AuctionItem();
        item.setId(rs.getInt("id"));
        item.setTitle(rs.getString("title"));
        item.setDescription(rs.getString("description"));
        item.setCategory(rs.getString("category"));
        item.setStartingPrice(rs.getDouble("starting_price"));
        item.setCurrentBid(rs.getDouble("current_bid"));
        item.setHighestBidderUsername(rs.getString("highest_bidder_username"));
        item.setSellerUsername(rs.getString("seller_username"));
        item.setStartTime(rs.getString("start_time"));
        item.setEndTime(rs.getString("end_time"));
        item.setStatus(rs.getString("status"));
        item.setImg1(rs.getString("img1"));
        item.setImg2(rs.getString("img2"));
        item.setImg3(rs.getString("img3"));
        return item;
    }

    public AuctionItem findAuctionById(int id) {
        String sql = "SELECT * FROM auction_items WHERE id = ?";
        try (var pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (var rs = pstmt.executeQuery()) {
                if (rs.next()) return mapRowToAuction(rs);
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to find auction", e);
        }
        return null;
    }

    public List<AuctionItem> findActiveAuctions() {
        List<AuctionItem> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM auction_items WHERE status = 'ACTIVE'";
        try (var stmt = connection.createStatement();
             var rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRowToAuction(rs));
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to fetch active auctions", e);
        }
        return list;
    }

    public List<AuctionItem> findAuctionsBySeller(String sellerUsername) {
        List<AuctionItem> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM auction_items WHERE seller_username = ?";
        try (var pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, sellerUsername);
            try (var rs = pstmt.executeQuery()) {
                while (rs.next()) list.add(mapRowToAuction(rs));
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to fetch seller auctions", e);
        }
        return list;
    }

    public void updateAuctionBid(int auctionId, double newBid, String bidderUsername) {
        String sql = "UPDATE auction_items SET current_bid = ?, highest_bidder_username = ? WHERE id = ?";
        try (var pstmt = connection.prepareStatement(sql)) {
            pstmt.setDouble(1, newBid);
            pstmt.setString(2, bidderUsername);
            pstmt.setInt(3, auctionId);
            pstmt.executeUpdate();
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to update bid", e);
        }
    }

    public void updateAuctionStatus(int auctionId, String status) {
        String sql = "UPDATE auction_items SET status = ? WHERE id = ?";
        try (var pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, auctionId);
            pstmt.executeUpdate();
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to update status", e);
        }
    }

    public void updateAuctionEndTime(int auctionId, String newEndTime) {
        String sql = "UPDATE auction_items SET end_time = ? WHERE id = ?";
        try (var pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, newEndTime);
            pstmt.setInt(2, auctionId);
            pstmt.executeUpdate();
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to update end time", e);
        }
    }

    public void updateAuctionImages(int auctionId, String img1, String img2, String img3) {
        String sql = "UPDATE auction_items SET img1 = ?, img2 = ?, img3 = ? WHERE id = ?";
        try (var pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, img1);
            pstmt.setString(2, img2);
            pstmt.setString(3, img3);
            pstmt.setInt(4, auctionId);
            pstmt.executeUpdate();
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to update images", e);
        }
    }
}
