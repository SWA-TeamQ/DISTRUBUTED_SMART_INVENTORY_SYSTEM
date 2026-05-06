package com.auction.shared.models;

import java.io.Serializable;

public abstract class User implements Serializable {
    private String username;
    private String password;
    private String roleType;

    protected User(String username, String password, String roleType) {
        this.username = username;
        this.password = password;
        this.roleType = roleType;
    }

    public String getUsername() { return username; }
    public String getRoleType() { return roleType; }
}
