package com.auction.shared.models;

import com.auction.shared.Constants;

public class Bidder extends User {
    private static final long serialVersionUID = 1L;

    public Bidder(String username, String passwordHash) {
        super(username, passwordHash, Constants.BIDDER);
    }
}
