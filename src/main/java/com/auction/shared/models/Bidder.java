package com.auction.shared.models;

public class Bidder extends User {
    public Bidder(String username, String password) {
        super(username, password, "BIDDER");
    }
}
