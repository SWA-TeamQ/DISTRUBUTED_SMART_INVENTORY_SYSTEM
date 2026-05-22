package com.auction.server.core;

import com.auction.server.repository.*;
import com.auction.server.service.*;
import com.auction.shared.Constants;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Connection;

/**
 * Handles the construction and wiring of server components.
 */
public class ServerBootstrap {

    private final AuctionServiceImpl service;
    private final UdpBroadcaster broadcaster;
    private final AuctionReaper reaper;
    private final com.auction.server.repository.DatabaseSyncService dbSyncService;
    private final String advertisedHost;

    public ServerBootstrap() throws Exception {
        // Initialize Logging
        com.auction.server.core.logging.AsyncLogger.initialize();

        this.advertisedHost = configureRmiHostname();

        // 1. Init Database
        DatabaseManager dbManager = new DatabaseManager();
        Connection conn = dbManager.getConnection();

        // 2. Init Repositories
        UserRepository userRepo = new UserRepository(conn);
        AuctionRepository auctionRepo = new AuctionRepository(conn);
        BidRepository bidRepo = new BidRepository(conn);

        // 3. Init Deep Core Modules
        TransactionManager txManager = new TransactionManager(conn);
        LockManager lockManager = new LockManager();
        
        AuctionManager auctionManager = new AuctionManager(auctionRepo, bidRepo, lockManager, txManager);
        LifecycleManager lifecycleManager = new LifecycleManager(auctionRepo, bidRepo, lockManager, txManager);
        ImageStore imageStore = new ImageStore(auctionRepo);

        // 4. Init Service
        this.service = new AuctionServiceImpl(userRepo, auctionManager, lifecycleManager, imageStore);

        // 5. Setup RMI
        int port = Constants.DEFAULT_RMI_PORT;
        Registry registry = LocateRegistry.createRegistry(port);
        registry.rebind(Constants.RMI_SERVICE_NAME, service);
        System.out.println("[RTDAS] RMI Registry bound: " + Constants.RMI_SERVICE_NAME + " on port " + port);

        // 6. Init Background Tasks
        this.reaper = new AuctionReaper(lifecycleManager);
        this.broadcaster = new UdpBroadcaster(advertisedHost, port, "MainServer");

        // 7. Start DB sync service to keep secondary DB files consistent
        this.dbSyncService = new com.auction.server.repository.DatabaseSyncService(new DatabaseManager(getDatabaseUrlForSync()));
        this.dbSyncService.start();
    }

    private String configureRmiHostname() {
        String configuredHost = System.getProperty("auction.rmi.hostname");
        if (configuredHost == null || configuredHost.isBlank()) {
            configuredHost = System.getenv("AUCTION_RMI_HOSTNAME");
        }
        if (configuredHost == null || configuredHost.isBlank()) {
            configuredHost = "localhost";
        }
        System.setProperty("java.rmi.server.hostname", configuredHost);
        System.out.println("[RTDAS] RMI hostname set to: " + configuredHost);
        return configuredHost;
    }

    public void start() {
        reaper.recoverFromCrash();
        reaper.start();
        broadcaster.start();
        System.out.println("[RTDAS] Auction Reaper and UDP Broadcaster started.");
        System.out.println("[RTDAS] Server is ready.");
    }

    public void stop() {
        broadcaster.stop();
        reaper.stop();
        try { dbSyncService.stop(); } catch (Exception ignored) {}
        System.out.println("[RTDAS] Server components stopped.");
    }

    // Helper to provide a DatabaseManager configured for the primary DB path
    private String getDatabaseUrlForSync() {
        return "jdbc:sqlite:" + Constants.DB_PATH;
    }
}
