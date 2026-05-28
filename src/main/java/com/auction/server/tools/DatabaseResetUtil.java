package com.auction.server.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;

import com.auction.server.repository.DatabaseManager;
import com.auction.shared.Constants;
import com.auction.server.util.SecurityUtil;


/**
 * Utility for resetting and seeding the RTDAS database with predictable test data.
 * Run this main method to prepare the environment for demonstrations.
 */
public class DatabaseResetUtil {

    public static void main(String[] args) {
        System.out.println("[DatabaseResetUtil] Starting database reset and seed...");
        DatabaseManager dbManager = new DatabaseManager();
        try (Connection conn = dbManager.getConnection()) {
            System.out.println("[DatabaseResetUtil] URL: " + conn.getMetaData().getURL());
            System.out.println("[DatabaseResetUtil] Resetting...");
            resetDatabase(conn);
            System.out.println("[DatabaseResetUtil] Initializing schema...");
            initSchema(conn);
            System.out.println("[DatabaseResetUtil] Seeding...");
            seedDatabase(conn);
            System.out.println("[DatabaseResetUtil] Success: Database reset and seeded with test data.");
        } catch (Exception e) {
            System.err.println("[DatabaseResetUtil] FAILED: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dbManager.close();
        }
    }

    private static void resetDatabase(Connection conn) throws SQLException {
        try (var stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS bids");
            stmt.execute("DROP TABLE IF EXISTS auction_items");
            stmt.execute("DROP TABLE IF EXISTS users");
        }
    }

    private static void initSchema(Connection conn) throws SQLException {
        try (var stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "username TEXT PRIMARY KEY, " +
                    "password_hash TEXT NOT NULL, " +
                    "role TEXT NOT NULL CHECK(role IN ('" + Constants.ADMIN + "','" + Constants.USER + "')), " +
                    "created_at TEXT NOT NULL" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS auction_items (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "title TEXT NOT NULL, " +
                    "description TEXT, " +
                    "category TEXT NOT NULL, " +
                    "starting_price_cents INTEGER NOT NULL CHECK(starting_price_cents >= 0), " +
                    "current_bid_cents INTEGER NOT NULL CHECK(current_bid_cents >= 0), " +
                    "highest_bidder_username TEXT, " +
                    "seller_username TEXT NOT NULL, " +
                    "start_time TEXT NOT NULL, " +
                    "end_time TEXT NOT NULL, " +
                    "cap_end_time TEXT NOT NULL, " +
                    "status TEXT NOT NULL CHECK(status IN ('ACTIVE','SOLD','EXPIRED','CANCELLED')), " +
                    "img1 TEXT, img2 TEXT, img3 TEXT, " +
                    "relisted_from INTEGER, " +
                    "FOREIGN KEY (seller_username) REFERENCES users(username), " +
                    "FOREIGN KEY (relisted_from) REFERENCES auction_items(id)" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS bids (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "auction_item_id INTEGER NOT NULL, " +
                    "bidder_username TEXT NOT NULL, " +
                    "amount_cents INTEGER NOT NULL CHECK(amount_cents > 0), " +
                    "timestamp TEXT NOT NULL, " +
                    "FOREIGN KEY (auction_item_id) REFERENCES auction_items(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (bidder_username) REFERENCES users(username)" +
                    ")");
        }
    }

    private static void seedDatabase(Connection conn) throws SQLException {
        String now = Instant.now().toString();

        // 1. Create Users
        try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users VALUES (?, ?, ?, ?)")) {
            String[][] users = {
                    {"admin", SecurityUtil.hashPassword("admin"), Constants.ADMIN, now},
                    {"bidder", SecurityUtil.hashPassword("password"), Constants.USER, now},
                    {"seller", SecurityUtil.hashPassword("password"), Constants.USER, now},
                    {"winner", SecurityUtil.hashPassword("password"), Constants.USER, now}
            };
            for (String[] user : users) {
                pstmt.setString(1, user[0]);
                pstmt.setString(2, user[1]);
                pstmt.setString(3, user[2]);
                pstmt.setString(4, user[3]);
                pstmt.executeUpdate();
            }
        }

        // 2. Create Auctions
        String auctionSql = "INSERT INTO auction_items (title, description, category, starting_price_cents, current_bid_cents, seller_username, start_time, end_time, cap_end_time, status) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(auctionSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            Object[][] auctions = {
                    {"Vintage Camera", "Classic film camera", "ELECTRONICS", 5000, 5500, "admin", now, "2099-01-01T00:00:00", "2099-01-01T00:00:00", Constants.STATUS_ACTIVE},
                    {"Modern Laptop", "Fast machine", "ELECTRONICS", 100000, 105000, "admin", now, "2099-01-01T00:00:00", "2099-01-01T00:00:00", Constants.STATUS_ACTIVE},
                    {"Old Clock", "Antique desk clock", "HOME", 2000, 2500, "admin", now, "2000-01-01T00:00:00", "2000-01-01T00:00:00", Constants.STATUS_SOLD},
                    {"Empty Vase", "Rare ceramic", "HOME", 1000, 1000, "admin", now, "2000-01-01T00:00:00", "2000-01-01T00:00:00", Constants.STATUS_EXPIRED},
                    {"Broken Table", "Wood base", "FURNITURE", 500, 500, "admin", now, "2099-01-01T00:00:00", "2099-01-01T00:00:00", Constants.STATUS_CANCELLED}
            };

            for (Object[] auction : auctions) {
                for (int i = 0; i < auction.length; i++) pstmt.setObject(i + 1, auction[i]);
                pstmt.executeUpdate();
                
                // Add some bids for the first 3
                try (var rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        if (id <= 3) addBids(conn, id);
                    }
                }
            }
        }
    }

    private static void addBids(Connection conn, int auctionId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO bids (auction_item_id, bidder_username, amount_cents, timestamp) VALUES (?,?,?,?)")) {
            String now = Instant.now().toString();
            for (int i = 1; i <= 3; i++) {
                pstmt.setInt(1, auctionId);
                pstmt.setString(2, "bidder");
                pstmt.setInt(3, 1000 * i);
                pstmt.setString(4, now);
                pstmt.executeUpdate();
            }
        }
    }
}
