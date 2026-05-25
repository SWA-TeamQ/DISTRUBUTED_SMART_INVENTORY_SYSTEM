package com.auction.server.core;

import com.auction.server.repository.*;
import com.auction.server.service.*;
import com.auction.shared.Constants;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.sql.Connection;

/**
 * Handles the construction and wiring of server components.
 */
public class ServerBootstrap {

    private final AuctionServiceImpl service;
    private final UdpBroadcaster broadcaster;
    private final AuctionReaper reaper;

    public ServerBootstrap() throws Exception {
        // Initialize Logging
        com.auction.server.core.logging.AsyncLogger.initialize();

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

        SessionManager sessionManager = new SessionManager(userRepo);
        AdminManager adminManager = new AdminManager(auctionManager, userRepo);

        // 4. Init Service
        this.service = new AuctionServiceImpl(auctionManager, sessionManager, adminManager, imageStore);

        // 5. Setup RMI
        int port = Constants.DEFAULT_RMI_PORT;
        Registry registry;
        try {
            registry = LocateRegistry.createRegistry(port);
            System.out.println("[RTDAS] Created RMI Registry on port " + port);
        } catch (ExportException e) {
            registry = LocateRegistry.getRegistry(port);
            System.out.println("[RTDAS] Reusing existing RMI Registry on port " + port);
        }
        registry.rebind(Constants.RMI_SERVICE_NAME, service);
        System.out.println("[RTDAS] RMI Registry bound: " + Constants.RMI_SERVICE_NAME + " on port " + port);

        // 6. Init Background Tasks
        this.reaper = new AuctionReaper(lifecycleManager);
        this.broadcaster = new UdpBroadcaster(port, "MainServer");
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
        System.out.println("[RTDAS] Server components stopped.");
    }
}
