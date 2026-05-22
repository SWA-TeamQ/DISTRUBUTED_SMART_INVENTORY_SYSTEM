package com.auction.shared.models;

import java.io.Serializable;

/**
 * Base model for application users.
 * Admin is the privileged role; all normal accounts are USER.
 * Password stored as SHA-256 hash — never plaintext.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String passwordHash;
    private String roleType;

    public User(String username, String passwordHash, String roleType) {
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
