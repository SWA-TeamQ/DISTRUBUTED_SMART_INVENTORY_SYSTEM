package com.auction.server.repository;

import com.auction.shared.Constants;
import com.auction.shared.models.User;
import com.auction.shared.models.Admin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    private final Connection connection;

    public UserRepository(Connection connection) {
        this.connection = connection;
        seedDefaultAdmin();
    }

    public Connection getConnection() {
        return connection;
    }

    private void seedDefaultAdmin() {
        try (var stmt = connection.createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.next() && rs.getInt(1) == 0) {
                insertUser(Constants.DEFAULT_ADMIN_USERNAME,
                        com.auction.server.util.SecurityUtil.hashPassword(Constants.DEFAULT_ADMIN_PASSWORD),
                        Constants.ADMIN);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to seed admin", e);
        }
    }

    public User findUserByUsername(String username) {
        String sql = "SELECT password_hash, role FROM users WHERE username = ?";
        try (var pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (var rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String p = rs.getString("password_hash");
                    String r = rs.getString("role");
                    if (Constants.ADMIN.equals(r)) return new Admin(username, p);
                    return new User(username, p, Constants.USER);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user", e);
        }
        return null;
    }

    public void insertUser(String username, String passwordHash, String role) {
        String sql = "INSERT INTO users (username, password_hash, role, created_at) VALUES (?, ?, ?, ?)";
        try (var pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            pstmt.setString(3, role);
            pstmt.setString(4, java.time.Instant.now().toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert user", e);
        }

        // Also attempt to insert into the secondary DB file (auction.db.sqlite)
        String secondaryPath = Constants.DB_PATH + ".sqlite";
        String secondaryUrl = "jdbc:sqlite:" + secondaryPath;
        String sqlIgnore = "INSERT OR IGNORE INTO users (username, password_hash, role, created_at) VALUES (?, ?, ?, ?)";
        try (var secConn = java.sql.DriverManager.getConnection(secondaryUrl)) {
            try {
                ensureUsersSchema(secConn);
            } catch (SQLException e) {
                System.out.println("[UserRepository] Warning: failed to ensure users schema on secondary DB (" + secondaryPath + "): " + e.getMessage());
            }

            try (var pstmt2 = secConn.prepareStatement(sqlIgnore)) {
                pstmt2.setString(1, username);
                pstmt2.setString(2, passwordHash);
                pstmt2.setString(3, role);
                pstmt2.setString(4, java.time.Instant.now().toString());
                pstmt2.executeUpdate();
            }
        } catch (SQLException e) {
            // Secondary DB may not exist or be locked; log and continue without failing registration
            System.out.println("[UserRepository] Warning: failed to write user to secondary DB (" + secondaryPath + "): " + e.getMessage());
        }
    }

    private void ensureUsersSchema(java.sql.Connection conn) throws SQLException {
        try (var stmt = conn.createStatement()) {
            // If users table does not exist, create it with the current schema
            try (var rs = stmt.executeQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name='users'")) {
                if (!rs.next()) {
                    stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                            "username TEXT PRIMARY KEY, " +
                            "password_hash TEXT NOT NULL, " +
                            "role TEXT NOT NULL, " +
                            "created_at TEXT NOT NULL")
                    ;
                    return;
                }
            }

            // Ensure created_at column exists (can be added via ALTER TABLE)
            try (var rs2 = stmt.executeQuery("PRAGMA table_info(users)")) {
                boolean hasCreatedAt = false;
                while (rs2.next()) {
                    String col = rs2.getString("name");
                    if ("created_at".equalsIgnoreCase(col)) {
                        hasCreatedAt = true;
                        break;
                    }
                }
                if (!hasCreatedAt) {
                    stmt.executeUpdate("ALTER TABLE users ADD COLUMN created_at TEXT");
                }
            }

            // Normalize roles to USER where necessary
            stmt.executeUpdate("UPDATE users SET role='USER' WHERE role IS NULL OR role NOT IN ('" + Constants.ADMIN + "','" + Constants.USER + "')");
        }
    }

    public List<User> findAllUsers() {
        List<User> users = new ArrayList<>();
        try (var stmt = connection.createStatement();
             var rs = stmt.executeQuery("SELECT username, password_hash, role FROM users")) {
            while (rs.next()) {
                String u = rs.getString("username");
                String p = rs.getString("password_hash");
                String r = rs.getString("role");
                if (Constants.ADMIN.equals(r)) users.add(new Admin(u, p));
                else users.add(new User(u, p, Constants.USER));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch users", e);
        }
        return users;
    }
}
