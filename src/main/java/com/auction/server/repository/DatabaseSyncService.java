package com.auction.server.repository;

import com.auction.shared.Constants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically synchronizes the `users` table from the primary DB connection to
 * the known secondary DB files (auction.db and auction.db.sqlite). This is a
 * lightweight, best-effort sync that uses INSERT OR REPLACE to keep rows in
 * sync. It logs warnings on failures but never prevents normal operation.
 */
public class DatabaseSyncService {
    private final DatabaseManager dbManager;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "DBSyncService");
        t.setDaemon(true);
        return t;
    });

    public DatabaseSyncService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void start() {
        // Run immediately, then every 60 seconds
        scheduler.scheduleAtFixedRate(this::syncOnce, 0, 60, TimeUnit.SECONDS);
    }

    public void stop() {
        try { scheduler.shutdownNow(); } catch (Exception ignored) {}
    }

    private void syncOnce() {
        try (Connection primary = dbManager.getConnection()) {
            List<UserRow> users = readUsers(primary);

            // Destinations: workspace root data files
            String[] dests = new String[] { "data/auction.db", "data/auction.db.sqlite" };

            for (String dest : dests) {
                try {
                    String abs;
                    try { abs = new java.io.File(dest).getCanonicalPath(); } catch (Exception e) { abs = new java.io.File(dest).getAbsolutePath(); }
                    // If the destination equals the primary DB file, skip
                    String primaryPath = getPrimaryDbPath(primary);
                    if (primaryPath != null && primaryPath.equals(abs)) continue;

                    String url = "jdbc:sqlite:" + abs;
                    try (var conn = java.sql.DriverManager.getConnection(url)) {
                        conn.setAutoCommit(false);
                        try (Statement st = conn.createStatement()) {
                            st.execute("CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY, password_hash TEXT NOT NULL, role TEXT NOT NULL CHECK(role IN ('" + Constants.ADMIN + "','" + Constants.USER + "')), created_at TEXT NOT NULL)");
                        }

                        try (PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO users (username, password_hash, role, created_at) VALUES (?, ?, ?, ?)")) {
                            for (UserRow u : users) {
                                ps.setString(1, u.username);
                                ps.setString(2, u.passwordHash);
                                ps.setString(3, u.role);
                                ps.setString(4, u.createdAt);
                                ps.addBatch();
                            }
                            ps.executeBatch();
                        }
                        conn.commit();
                    }
                } catch (Exception e) {
                    System.out.println("[DatabaseSyncService] Warning: failed to sync to " + dest + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("[DatabaseSyncService] Warning: failed to read primary users: " + e.getMessage());
        }
    }

    private List<UserRow> readUsers(Connection conn) throws Exception {
        List<UserRow> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT username, password_hash, role, COALESCE(created_at, ?) as created_at FROM users")) {
            ps.setString(1, Instant.now().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String username = rs.getString("username");
                    String ph = rs.getString("password_hash");
                    String role = rs.getString("role");
                    if (!Constants.ADMIN.equals(role)) role = Constants.USER;
                    String createdAt = rs.getString("created_at");
                    out.add(new UserRow(username, ph, role, createdAt));
                }
            }
        }
        return out;
    }

    private String getPrimaryDbPath(Connection conn) {
        try {
            // Attempt to read PRAGMA database_list to get filename
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("PRAGMA database_list")) {
                while (rs.next()) {
                    String file = rs.getString("file");
                    if (file != null && !file.trim().isEmpty()) {
                        try { return new java.io.File(file).getCanonicalPath(); } catch (Exception e) { return new java.io.File(file).getAbsolutePath(); }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static final class UserRow {
        final String username;
        final String passwordHash;
        final String role;
        final String createdAt;

        UserRow(String username, String passwordHash, String role, String createdAt) {
            this.username = username;
            this.passwordHash = passwordHash;
            this.role = role;
            this.createdAt = createdAt;
        }
    }
}
