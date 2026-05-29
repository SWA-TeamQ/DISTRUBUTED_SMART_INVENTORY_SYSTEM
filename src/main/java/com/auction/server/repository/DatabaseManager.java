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
    private final String dbUrl;
    private final String configuredPath;

    public DatabaseManager() {
        this("jdbc:sqlite:" + Constants.DB_PATH);
    }

    public DatabaseManager(String dbUrl) {
        this.dbUrl = dbUrl;
        String sqlitePrefix = "jdbc:sqlite:";
        if (dbUrl.startsWith(sqlitePrefix)) {
            this.configuredPath = dbUrl.substring(sqlitePrefix.length());
        } else {
            this.configuredPath = null;
        }

        bootstrapDirectories();
        
        try {
            connection = DriverManager.getConnection(dbUrl);
            try (var stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
            // If an existing DB uses the old auction_items schema, apply migration
            applyScheduledMigrationIfNeeded();
            applyStartModeColumnMigrationIfNeeded();
            initSchema();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void applyStartModeColumnMigrationIfNeeded() {
        try (var stmt = connection.createStatement()) {
            boolean hasColumn = false;
            try (var rs = stmt.executeQuery("PRAGMA table_info(auction_items)")) {
                while (rs.next()) {
                    String col = rs.getString("name");
                    if ("start_mode".equalsIgnoreCase(col)) {
                        hasColumn = true;
                        break;
                    }
                }
            }
            if (!hasColumn) {
                stmt.execute("ALTER TABLE auction_items ADD COLUMN start_mode TEXT NOT NULL DEFAULT 'AUTO'");
                System.out.println("[RTDAS] Added start_mode column to auction_items");
            }
            // Ensure min_increment_percent exists (double, default to global constant)
            boolean hasMinInc = false;
            try (var rs2 = stmt.executeQuery("PRAGMA table_info(auction_items)")) {
                while (rs2.next()) {
                    String col = rs2.getString("name");
                    if ("min_increment_percent".equalsIgnoreCase(col)) {
                        hasMinInc = true;
                        break;
                    }
                }
            }
            if (!hasMinInc) {
                stmt.execute("ALTER TABLE auction_items ADD COLUMN min_increment_percent REAL NOT NULL DEFAULT " + Constants.MIN_BID_INCREMENT_PERCENT);
                System.out.println("[RTDAS] Added min_increment_percent column to auction_items");
            }
        } catch (Exception e) {
            System.err.println("[RTDAS] start_mode migration check failed: " + e.getMessage());
        }
    }

    /**
     * Detects if `auction_items` table allows SCHEDULED status; if not, performs an in-place migration
     * (backups the DB file and rebuilds the table with the new schema while preserving data).
     */
    private void applyScheduledMigrationIfNeeded() {
        if (configuredPath == null) return;
        try (var stmt = connection.createStatement()) {
            try (var rs = stmt.executeQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name='auction_items'")) {
                if (rs.next()) {
                    String sql = rs.getString("sql");
                    if (sql != null && sql.contains("SCHEDULED")) {
                        // already migrated
                        return;
                    }
                } else {
                    // No table yet, nothing to migrate
                    return;
                }
            }

            // Backup current DB file
            try {
                Path src = Path.of(configuredPath);
                if (Files.exists(src)) {
                    Path backup = src.resolveSibling(src.getFileName().toString() + ".bak." + System.currentTimeMillis());
                    Files.copy(src, backup);
                    System.out.println("[RTDAS] Backed up DB to " + backup.toString());
                }
            } catch (Exception e) {
                System.err.println("[RTDAS] DB backup failed: " + e.getMessage());
            }

            // Run migration statements
            String[] stmts = new String[] {
                "CREATE TABLE IF NOT EXISTS auction_items_new (" +
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
                    "cap_end_time TEXT, " +
                    "status TEXT NOT NULL CHECK(status IN ('SCHEDULED','ACTIVE','SOLD','EXPIRED','CANCELLED')), " +
                    "start_mode TEXT NOT NULL DEFAULT 'AUTO', " +
                    "min_increment_percent REAL NOT NULL DEFAULT " + Constants.MIN_BID_INCREMENT_PERCENT + ", " +
                    "img1 TEXT, img2 TEXT, img3 TEXT, " +
                    "relisted_from INTEGER" +
                    ")",
                "INSERT INTO auction_items_new (id, title, description, category, starting_price_cents, current_bid_cents, " +
                    "highest_bidder_username, seller_username, start_time, end_time, cap_end_time, status, start_mode, min_increment_percent, img1, img2, img3, relisted_from) " +
                    "SELECT id, title, description, category, starting_price_cents, current_bid_cents, " +
                    "highest_bidder_username, seller_username, start_time, end_time, cap_end_time, status, 'AUTO', " + Constants.MIN_BID_INCREMENT_PERCENT + ", img1, img2, img3, relisted_from FROM auction_items",
                "DROP TABLE auction_items",
                "ALTER TABLE auction_items_new RENAME TO auction_items",
                "CREATE INDEX IF NOT EXISTS idx_bids_auction_id ON bids(auction_item_id)",
                "CREATE INDEX IF NOT EXISTS idx_auction_status_end ON auction_items(status, end_time)",
                "CREATE INDEX IF NOT EXISTS idx_auction_seller ON auction_items(seller_username)"
            };

            try {
                connection.setAutoCommit(false);
                try (var s = connection.createStatement()) {
                    // Temporarily disable foreign key enforcement for the copy
                    s.execute("PRAGMA foreign_keys = OFF");
                    for (String m : stmts) {
                        s.execute(m);
                    }
                    // Re-enable foreign keys
                    s.execute("PRAGMA foreign_keys = ON");
                }
                connection.commit();
                System.out.println("[RTDAS] Applied scheduled-auction migration");
            } catch (Exception e) {
                try { connection.rollback(); } catch (Exception ignored) {}
                System.err.println("[RTDAS] Migration failed: " + e.getMessage());
            } finally {
                try { connection.setAutoCommit(true); } catch (Exception ignored) {}
            }

        } catch (Exception e) {
            System.err.println("[RTDAS] Migration check failed: " + e.getMessage());
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
                    "cap_end_time TEXT, " +
                    "status TEXT NOT NULL CHECK(status IN ('SCHEDULED','ACTIVE','SOLD','EXPIRED','CANCELLED')), " +
                    "start_mode TEXT NOT NULL DEFAULT 'AUTO', " +
                    "min_increment_percent REAL NOT NULL DEFAULT " + Constants.MIN_BID_INCREMENT_PERCENT + ", " +
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
