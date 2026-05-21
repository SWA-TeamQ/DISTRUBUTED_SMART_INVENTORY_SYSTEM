package com.auction.server.core;

/**
 * Server entry point.
 */
public class ServerLauncher {

    public static void main(String[] args) {
        try {
            System.out.println("[RTDAS] Initializing server...");
            
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.start();
            
        } catch (Exception e) {
            System.err.println("[RTDAS] Server failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

