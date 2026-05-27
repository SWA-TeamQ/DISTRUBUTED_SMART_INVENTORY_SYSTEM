package com.auction.client.tools;

import com.auction.shared.Constants;
import com.auction.shared.interfaces.IAuctionService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Small utility to exercise register/login against a running RTDAS server.
 */
public class TestRegisterLogin {
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = Constants.DEFAULT_RMI_PORT;
        if (args.length >= 1) host = args[0];
        if (args.length >= 2) port = Integer.parseInt(args[1]);

        System.out.println("Connecting to RMI registry at " + host + ":" + port);
        Registry reg = LocateRegistry.getRegistry(host, port);
        IAuctionService svc = (IAuctionService) reg.lookup(Constants.RMI_SERVICE_NAME);

        String username = "test_user_" + System.currentTimeMillis()%10000;
        String password = "secret";
        try {
            svc.register(username, password, Constants.USER);
            System.out.println("Registered: " + username);
        } catch (Exception e) {
            System.out.println("Register failed: " + e.getMessage());
        }

        String token = null;
        try {
            token = svc.login(username, password);
            System.out.println("Logged in, token=" + token);
        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        try {
            String role = svc.getMyRole(token);
            System.out.println("Role: " + role);
        } catch (Exception e) {
            System.out.println("getMyRole failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("Test complete.");
    }
}
