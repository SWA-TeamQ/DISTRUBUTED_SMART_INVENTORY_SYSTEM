package com.inventory.shared.models;

public class Staff extends User {
    public Staff(String username, String password) {
        super(username, password, "STAFF");
    }
}
