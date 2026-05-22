package com.auction.tools;

import com.auction.shared.Constants;
import com.auction.shared.interfaces.IAuctionService;

import java.rmi.registry.LocateRegistry;
import java.time.Instant;

public class TestRegisterLogin {
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 1999;
        IAuctionService service = (IAuctionService) LocateRegistry.getRegistry(host, port).lookup(Constants.RMI_SERVICE_NAME);
        String username = "auto_user_" + Instant.now().toEpochMilli();
        String password = "pass123";
        try {
            service.register(username, password, Constants.USER);
            System.out.println("REGISTERED " + username);
        } catch (Exception e) {
            System.out.println("Register failed: " + e.getMessage());
        }
        String token = null;
        try {
            token = service.login(username, password);
            System.out.println("LOGGED_IN token=" + token);
            String role = service.getSessionRole(token);
            System.out.println("ROLE=" + role);
        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
