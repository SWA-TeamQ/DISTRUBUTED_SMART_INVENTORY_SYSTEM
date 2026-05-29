package com.auction.server.core;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.sql.Connection;

import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.BidRepository;
import com.auction.server.repository.DatabaseManager;
import com.auction.server.repository.UserRepository;
import com.auction.server.service.AuctionReaper;
import com.auction.server.service.AuctionServiceImpl;
import com.auction.shared.Constants;

/**
 * Handles the construction and wiring of server components.
 */
public class ServerBootstrap {

  private final AuctionServiceImpl service;
  private final UdpBroadcaster broadcaster;
  private final AuctionReaper reaper;
  private final int rmiPort;

  public ServerBootstrap() throws Exception {
    this(Constants.DEFAULT_RMI_PORT);
  }

  public ServerBootstrap(int rmiPort) throws Exception {
    this.rmiPort = rmiPort;

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

    AuctionManager auctionManager = new AuctionManager(
      auctionRepo,
      bidRepo,
      lockManager,
      txManager
    );
    LifecycleManager lifecycleManager = new LifecycleManager(
      auctionRepo,
      bidRepo,
      lockManager,
      txManager
    );
    ImageStore imageStore = new ImageStore();
    AdminManager adminManager = new AdminManager(auctionManager, userRepo);

    // 4. Init Service
    this.service = new AuctionServiceImpl(
      userRepo,
      auctionManager,
      adminManager,
      imageStore
    );

    // 5. Setup RMI
    Registry registry;
    try {
      registry = LocateRegistry.createRegistry(this.rmiPort);
      System.out.println(
        "[RTDAS] Created RMI Registry on port " + this.rmiPort
      );
    } catch (ExportException e) {
      registry = LocateRegistry.getRegistry(this.rmiPort);
      System.out.println(
        "[RTDAS] Reusing existing RMI Registry on port " + this.rmiPort
      );
    }
    registry.rebind(Constants.RMI_SERVICE_NAME, service);
    System.out.println(
      "[RTDAS] RMI Registry bound: " +
        Constants.RMI_SERVICE_NAME +
        " on port " +
        this.rmiPort
    );

    // 6. Init Background Tasks
    this.reaper = new AuctionReaper(lifecycleManager);
    this.broadcaster = new UdpBroadcaster(this.rmiPort, "MainServer");
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
