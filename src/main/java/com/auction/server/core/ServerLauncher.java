package com.auction.server.core;

import java.util.concurrent.CountDownLatch;

/**
 * Server entry point.
 */
public class ServerLauncher {

    public static void main(String[] args) {
        CountDownLatch shutdownLatch = new CountDownLatch(1);
        ServerBootstrap[] bootstrapHolder = new ServerBootstrap[1];

        System.out.println("[RTDAS] Starting server process...");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ServerBootstrap bootstrap = bootstrapHolder[0];
            if (bootstrap != null) {
                bootstrap.stop();
            }
            shutdownLatch.countDown();
        }, "RTDAS-ShutdownHook"));

        Thread serverLifecycleThread = new Thread(() -> {
            try {
                System.out.println("[RTDAS] Initializing server...");

                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrapHolder[0] = bootstrap;
                bootstrap.start();

                System.out.println("[RTDAS] Server is running. Press Ctrl+C to stop.");
                shutdownLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("[RTDAS] Server failed to start: " + e.getMessage());
                e.printStackTrace();
                shutdownLatch.countDown();
            }
        }, "RTDAS-ServerLifecycle");
        serverLifecycleThread.setDaemon(false);
        serverLifecycleThread.start();
    }
}

