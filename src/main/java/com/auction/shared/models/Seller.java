package com.auction.shared.models;
import com.auction.shared.Constants;

public class Seller extends User {
    public Seller(String username, String password) {
        super(username, password, Constants.SELLER);
    }
}
