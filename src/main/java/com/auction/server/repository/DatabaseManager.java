package com.auction.server.repository;

import com.auction.shared.Constants;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages SQLite connection and schema lifecycle.
 */
public class DatabaseManager {

    private Connection connection;

    public DatabaseManager() {
        this(resolveDefaultDbUrl());
    }

    private static String resolveDefaultDbUrl() {
        try {
            String dbFilePath = new java.io.File(Constants.DB_PATH).getCanonicalPath();
            return "jdbc:sqlite:" + dbFilePath;
        } catch (java.io.IOException e) {
            return "jdbc:sqlite:" + new java.io.File(Constants.DB_PATH).getAbsolutePath();
        }
    }

    public DatabaseManager(String dbUrl) {
        bootstrapDirectories();
        try {
            connection = DriverManager.getConnection(dbUrl);
            // Log the resolved DB file path for determinism and debugging
            String dbPathDisplay = dbUrl.startsWith("jdbc:sqlite:") ? dbUrl.substring("jdbc:sqlite:".length()) : dbUrl;
            try {
                String canonical = new java.io.File(dbPathDisplay).getCanonicalPath();
                System.out.println("[RTDAS] Using database file: " + canonical);
            } catch (java.io.IOException ignored) {
                System.out.println("[RTDAS] Using database url: " + dbUrl);
            }
            try (var stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
            initSchema();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
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

<<<<<<< HEAD
        migrateUsersSchema();
    }

    private void migrateUsersSchema() throws SQLException {
        boolean hasCreatedAt = false;
        boolean hasLegacyRoleConstraint = false;

        try (var pstmt = connection.prepareStatement(
                "SELECT sql FROM sqlite_master WHERE type='table' AND name='users'");
             var rs = pstmt.executeQuery()) {
            if (rs.next()) {
                String tableSql = rs.getString("sql");
                if (tableSql != null) {
                    String normalizedSql = tableSql.toUpperCase();
                    hasLegacyRoleConstraint = normalizedSql.contains("SELLER") || normalizedSql.contains("BIDDER");
                }
            }
        }

        try (var stmt = connection.createStatement();
             var rs = stmt.executeQuery("PRAGMA table_info(users)")) {
            while (rs.next()) {
                String columnName = rs.getString("name");
                if ("created_at".equalsIgnoreCase(columnName)) {
                    hasCreatedAt = true;
                    break;
                }
            }
        }

        if (hasLegacyRoleConstraint || !hasCreatedAt) {
            String createdAtSelect = hasCreatedAt ? "COALESCE(created_at, datetime('now'))" : "datetime('now')";

            try (var stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = OFF");
                stmt.execute("BEGIN TRANSACTION");

                stmt.execute("ALTER TABLE users RENAME TO users_legacy");
                stmt.execute("CREATE TABLE users (" +
                        "username TEXT PRIMARY KEY, " +
                        "password_hash TEXT NOT NULL, " +
                        "role TEXT NOT NULL CHECK(role IN ('" + Constants.ADMIN + "','" + Constants.USER + "')), " +
                        "created_at TEXT NOT NULL" +
                        ")");

                stmt.execute("INSERT INTO users (username, password_hash, role, created_at) " +
                        "SELECT username, password_hash, " +
                        "CASE WHEN role='" + Constants.ADMIN + "' THEN '" + Constants.ADMIN + "' ELSE '" + Constants.USER + "' END, " +
                        createdAtSelect + " FROM users_legacy");

                stmt.execute("DROP TABLE users_legacy");
                stmt.execute("COMMIT");
                stmt.execute("PRAGMA foreign_keys = ON");
            }
            return;
        }

        try (var stmt = connection.createStatement()) {
            stmt.executeUpdate(
                "UPDATE users SET role='" + Constants.USER + "' " +
                "WHERE role IS NULL OR role NOT IN ('" + Constants.ADMIN + "','" + Constants.USER + "')"
            );
            stmt.executeUpdate(
                "UPDATE users SET created_at = COALESCE(created_at, datetime('now')) " +
                "WHERE created_at IS NULL OR TRIM(created_at) = ''"
            );
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try { if (connection != null) connection.close(); } catch (SQLException ignored) {}
    }
}
