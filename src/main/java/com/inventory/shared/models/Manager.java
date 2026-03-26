package com.inventory.shared.models;

public class Manager extends User {
    public Manager(String username, String password) {
        super(username, password, "MANAGER");
    }
}
