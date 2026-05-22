package com.auction.shared.models;

import com.auction.shared.Constants;

public class Admin extends User {
    private static final long serialVersionUID = 1L;

    public Admin(String username, String passwordHash, String createdAt) {
        super(username, passwordHash, Constants.ADMIN, createdAt);
    }
}
