package com.auction.shared.models;

import java.io.Serializable;

/**
 * Unified user model.
 * Roles are ADMIN or USER; auction-side participation is contextual per auction.
 * Password stored as SHA-256 hash — never plaintext.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String passwordHash;
    private String roleType;
    private String createdAt;

    public User(String username, String passwordHash, String roleType, String createdAt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.roleType = roleType;
        this.createdAt = createdAt;
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getRoleType() { return roleType; }
    public String getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return roleType + "[" + username + "]";
    }
}
