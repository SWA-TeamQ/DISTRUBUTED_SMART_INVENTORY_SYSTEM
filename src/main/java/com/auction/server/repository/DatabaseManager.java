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
        this("jdbc:sqlite:" + Constants.DB_PATH);
    }

    public DatabaseManager(String dbUrl) {
        try {
            connection = DriverManager.getConnection(dbUrl);
            initSchema();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void initSchema() throws SQLException {
        try (var stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "username TEXT PRIMARY KEY, " +
                    "password_hash TEXT NOT NULL, " +
                    "role TEXT NOT NULL CHECK(role IN ('" + Constants.ADMIN + "','" + Constants.SELLER + "','" + Constants.BIDDER + "'))" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS auction_items (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "title TEXT NOT NULL, " +
                    "description TEXT, " +
                    "category TEXT NOT NULL, " +
                    "starting_price REAL NOT NULL CHECK(starting_price >= 0), " +
                    "current_bid REAL NOT NULL, " +
                    "highest_bidder_username TEXT, " +
                    "seller_username TEXT NOT NULL, " +
                    "start_time TEXT NOT NULL, " +
                    "end_time TEXT NOT NULL, " +
                    "status TEXT NOT NULL CHECK(status IN ('ACTIVE','SOLD','EXPIRED','CANCELLED')), " +
                    "img1 TEXT, img2 TEXT, img3 TEXT, " +
                    "FOREIGN KEY (seller_username) REFERENCES users(username)" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS bids (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "auction_item_id INTEGER NOT NULL, " +
                    "bidder_username TEXT NOT NULL, " +
                    "amount REAL NOT NULL CHECK(amount > 0), " +
                    "timestamp TEXT NOT NULL, " +
                    "FOREIGN KEY (auction_item_id) REFERENCES auction_items(id)" +
                    ")");
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try { if (connection != null) connection.close(); } catch (SQLException ignored) {}
    }
}
