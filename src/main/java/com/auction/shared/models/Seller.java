package com.auction.shared.models;

import com.auction.shared.Constants;

public class Seller extends User {
    private static final long serialVersionUID = 1L;

    public Seller(String username, String passwordHash) {
        super(username, passwordHash, Constants.SELLER);
    }
}
