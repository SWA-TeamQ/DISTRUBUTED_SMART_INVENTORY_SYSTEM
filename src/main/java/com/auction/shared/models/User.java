package com.auction.shared.models;

import java.io.Serializable;

/**
 * Abstract base for all user types. Demonstrates OOP inheritance.
 * Subclasses: Admin, Seller, Bidder.
 * Password stored as SHA-256 hash — never plaintext.
 */
public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String passwordHash;
    private String roleType;

    protected User(String username, String passwordHash, String roleType) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.roleType = roleType;
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getRoleType() { return roleType; }

    @Override
    public String toString() {
        return roleType + "[" + username + "]";
    }
}
