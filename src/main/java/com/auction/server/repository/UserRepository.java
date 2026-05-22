package com.auction.server.repository;

import com.auction.shared.Constants;
import com.auction.shared.models.User;
import com.auction.shared.models.Admin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.time.Instant;

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
        String sql = "SELECT password_hash, role, created_at FROM users WHERE username = ?";
        try (var pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (var rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String p = rs.getString("password_hash");
                    String r = rs.getString("role");
                    String createdAt = rs.getString("created_at");
                    if (Constants.ADMIN.equals(r)) return new Admin(username, p, createdAt);
                    if (Constants.USER.equals(r)) return new User(username, p, Constants.USER, createdAt);
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
            pstmt.setString(4, Instant.now().toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert user", e);
        }
    }

    public List<User> findAllUsers() {
        List<User> users = new ArrayList<>();
        try (var stmt = connection.createStatement();
             var rs = stmt.executeQuery("SELECT username, password_hash, role, created_at FROM users")) {
            while (rs.next()) {
                String u = rs.getString("username");
                String p = rs.getString("password_hash");
                String r = rs.getString("role");
                String createdAt = rs.getString("created_at");
                if (Constants.ADMIN.equals(r)) users.add(new Admin(u, p, createdAt));
                else if (Constants.USER.equals(r)) users.add(new User(u, p, Constants.USER, createdAt));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch users", e);
        }
        return users;
    }
}
