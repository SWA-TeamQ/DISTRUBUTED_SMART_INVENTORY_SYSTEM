package com.auction.server.repository;

import com.auction.shared.Constants;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Manages SQLite connection and schema lifecycle.
 */
public class DatabaseManager {

    private Connection connection;

    public DatabaseManager() {
        this("jdbc:sqlite:" + Constants.DB_PATH);
    }

    public DatabaseManager(String dbUrl) {
        bootstrapDirectories();
        migrateLegacyDatabaseIfNeeded(dbUrl);
        try {
            connection = DriverManager.getConnection(dbUrl);
            try (var stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
            initSchema();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void migrateLegacyDatabaseIfNeeded(String dbUrl) {
        String sqlitePrefix = "jdbc:sqlite:";
        if (!dbUrl.startsWith(sqlitePrefix)) {
            return;
        }

        String configuredPath = dbUrl.substring(sqlitePrefix.length());
        Path configuredDbPath = Path.of(configuredPath);
        Path legacyDbPath = configuredDbPath.resolveSibling("auction.db");

        if (Files.exists(configuredDbPath) || !Files.exists(legacyDbPath)) {
            return;
        }

        try {
            Files.copy(legacyDbPath, configuredDbPath, StandardCopyOption.COPY_ATTRIBUTES);
            System.out.println("[RTDAS] Migrated legacy database from " + legacyDbPath + " to " + configuredDbPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to migrate legacy database", e);
        }
    }

    private void bootstrapDirectories() {
        new java.io.File("data").mkdirs();
        new java.io.File("logs").mkdirs();
        new java.io.File("resources/images").mkdirs();
        new java.io.File("resources/thumbs").mkdirs();
        new java.io.File("exports").mkdirs();
    }

    private void initSchema() throws SQLException {
        try (var stmt = connection.createStatement()) {
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

            // Create Indexes for search/reaper optimization
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bids_auction_id ON bids(auction_item_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_auction_status_end ON auction_items(status, end_time)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_auction_seller ON auction_items(seller_username)");
        }

    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try { if (connection != null) connection.close(); } catch (SQLException ignored) {}
    }
}
