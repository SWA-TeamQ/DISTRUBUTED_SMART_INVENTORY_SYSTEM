package com.auction.tools;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InspectDbTest {

    @Test
    public void clearAuctionData() throws Exception {
        new com.auction.server.repository.DatabaseManager("jdbc:sqlite:data/auction.db.sqlite");
        String url = "jdbc:sqlite:data/auction.db.sqlite";
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = OFF");
            stmt.executeUpdate("DELETE FROM bids");
            stmt.executeUpdate("DELETE FROM auction_items");
            stmt.executeUpdate("DELETE FROM sqlite_sequence WHERE name IN ('bids','auction_items')");
            stmt.execute("PRAGMA foreign_keys = ON");

            try (ResultSet rs1 = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM auction_items")) {
                if (rs1.next()) {
                    int auctionCount = rs1.getInt("cnt");
                    System.out.println("auction_items=" + auctionCount);
                    assertEquals(0, auctionCount, "auction_items should be empty after clearAuctionData");
                }
            }
            try (ResultSet rs2 = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM bids")) {
                if (rs2.next()) {
                    int bidCount = rs2.getInt("cnt");
                    System.out.println("bids=" + bidCount);
                    assertEquals(0, bidCount, "bids should be empty after clearAuctionData");
                }
            }
        }
    }

    @Test
    public void printAuctionItemsSchemaAndCount() throws Exception {
        // Initialize DatabaseManager to trigger any automatic migrations
        new com.auction.server.repository.DatabaseManager("jdbc:sqlite:data/auction.db.sqlite");
        String url = "jdbc:sqlite:data/auction.db.sqlite";
        try (Connection conn = DriverManager.getConnection(url)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name='auction_items'")) {
                    if (rs.next()) {
                        String sql = rs.getString("sql");
                        System.out.println("--- auction_items CREATE SQL ---");
                        System.out.println(sql);
                    } else {
                        System.out.println("auction_items table not found");
                    }
                }

                try (ResultSet rs2 = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM auction_items")) {
                    if (rs2.next()) {
                        System.out.println("--- auction_items ROW COUNT ---");
                        System.out.println(rs2.getInt("cnt"));
                    }
                } catch (Exception e) {
                    System.out.println("Could not count rows: " + e.getMessage());
                }
            }
        }
    }
}
