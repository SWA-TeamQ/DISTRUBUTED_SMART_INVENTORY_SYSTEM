package com.auction.server.core;

import com.auction.server.repository.AuctionRepository;

import com.auction.server.repository.BidRepository;
import com.auction.server.repository.DatabaseManager;
import com.auction.server.repository.UserRepository;
import com.auction.server.service.AuctionReaper;
import com.auction.server.service.AuctionServiceImpl;
import com.auction.shared.Constants;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Connection;

/**
 * Server entry point. Bootstraps RMI registry, binds AuctionService,
 * starts AuctionReaper, starts UDP broadcaster.
 */
public class ServerLauncher {

    public static void main(String[] args) {
        try {
            System.out.println("[RTDAS] Initializing server...");
            
            // Initialize Logging
            com.auction.server.core.logging.AsyncLogger.initialize();

            // 1. Init Database
            DatabaseManager dbManager = new DatabaseManager();
            Connection conn = dbManager.getConnection();


            // 2. Init Repositories
            UserRepository userRepo = new UserRepository(conn);
            AuctionRepository auctionRepo = new AuctionRepository(conn);
            BidRepository bidRepo = new BidRepository(conn);

            // 3. Init Deep Core Modules (Deepening Architecture)
            TransactionManager txManager = new TransactionManager(conn);
            LockManager lockManager = new LockManager();
            
            AuctionManager auctionManager = new AuctionManager(auctionRepo, bidRepo, lockManager, txManager);
            LifecycleManager lifecycleManager = new LifecycleManager(auctionRepo, bidRepo, lockManager, txManager);
            ImageStore imageStore = new ImageStore(auctionRepo);

            // 4. Init Service (Adapter)
            AuctionServiceImpl service = new AuctionServiceImpl(userRepo, auctionManager, lifecycleManager, imageStore);

            // 5. Setup RMI
            int port = Constants.DEFAULT_RMI_PORT;
            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind(Constants.RMI_SERVICE_NAME, service);
            System.out.println("[RTDAS] RMI Registry bound: " + Constants.RMI_SERVICE_NAME + " on port " + port);

            // 6. Start Background Tasks
            AuctionReaper reaper = new AuctionReaper(lifecycleManager);
            reaper.recoverFromCrash();
            reaper.start();
            System.out.println("[RTDAS] Auction Reaper started.");

            UdpBroadcaster broadcaster = new UdpBroadcaster(port, "MainServer");
            // broadcaster.start(); // Pending implementation in UdpBroadcaster

            System.out.println("[RTDAS] Server is ready.");
            
        } catch (Exception e) {
            System.err.println("[RTDAS] Server failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

