package com.auction.server.service;

import com.auction.shared.exceptions.AuctionException;
import com.auction.shared.interfaces.IAuctionService;
import com.auction.shared.models.AuctionItem;
import com.auction.shared.models.Bid;
import com.auction.shared.models.User;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.BidRepository;
import com.auction.server.repository.DatabaseManager;
import com.auction.server.repository.UserRepository;
import com.auction.server.repository.FileHandler;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * RMI service implementation. Extends UnicastRemoteObject for automatic RMI export.
 * Uses per-auction ReentrantLock for concurrency control on placeBid.
 */
public class AuctionServiceImpl extends UnicastRemoteObject implements IAuctionService {
    private static final long serialVersionUID = 1L;

    private final DatabaseManager dbManager;
    private final UserRepository userRepo;
    private final AuctionRepository auctionRepo;
    private final BidRepository bidRepo;
    private final FileHandler fileHandler;
    private final ImageManager imageManager;

    /** Per-auction locks for synchronized bidding. */
    private final ConcurrentHashMap<Integer, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    public AuctionServiceImpl(DatabaseManager dbManager, FileHandler fileHandler,
                               ImageManager imageManager) throws RemoteException {
        super();
        this.dbManager = dbManager;
        this.userRepo = new UserRepository(dbManager.getConnection());
        this.auctionRepo = new AuctionRepository(dbManager.getConnection());
        this.bidRepo = new BidRepository(dbManager.getConnection());
        this.fileHandler = fileHandler;
        this.imageManager = imageManager;
    }

    /** Gets or creates a lock for a specific auction ID. */
    private ReentrantLock getLock(int auctionId) {
        return auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock());
    }

    // --- Authentication ---
    @Override
    public User login(String username, String password) throws RemoteException {
        // TODO: hash password, query DB, return User subclass or null
        return null;
    }

    // --- Auction Browsing ---
    @Override
    public List<AuctionItem> getActiveAuctions() throws RemoteException {
        // TODO: query DB for status=ACTIVE
        return Collections.emptyList();
    }

    @Override
    public AuctionItem getAuctionById(int auctionId) throws RemoteException {
        // TODO: query DB by ID
        return null;
    }

    // --- Bidding ---
    @Override
    public void placeBid(int auctionId, String bidderUsername, double amount,
                          double clientExpectedPrice) throws RemoteException, AuctionException {
        ReentrantLock lock = getLock(auctionId);
        lock.lock();
        try {
            // TODO: validate (not seller, not duplicate, amount >= current*1.05,
            //        expectedPrice == currentBid, status==ACTIVE, now < endTime)
            // TODO: snipe protection (if endTime - now < 30s, extend 30s)
            // TODO: update DB, log to audit
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<Bid> getBidHistory(int auctionId) throws RemoteException {
        // TODO: query bids table ordered by timestamp
        return Collections.emptyList();
    }

    // --- Auction Management ---
    @Override
    public int createAuction(AuctionItem item, byte[] image1, byte[] image2, byte[] image3)
            throws RemoteException {
        // TODO: insert into DB, save images via ImageManager, return new ID
        return 0;
    }

    @Override
    public void cancelAuction(int auctionId, String sellerUsername)
            throws RemoteException, AuctionException {
        // TODO: verify seller owns auction, zero bids, set CANCELLED
    }

    // --- Image Handling ---
    @Override
    public byte[] getThumbnail(int auctionId, int imageIndex) throws RemoteException {
        // TODO: delegate to ImageManager
        return new byte[0];
    }

    @Override
    public byte[] getFullImage(int auctionId, int imageIndex) throws RemoteException {
        // TODO: delegate to ImageManager
        return new byte[0];
    }

    // --- Data Export ---
    @Override
    public byte[] exportAuctionsToCSV(String sellerUsername) throws RemoteException {
        // TODO: query seller's auctions, generate CSV via FileHandler
        return new byte[0];
    }

    // --- Administration ---
    @Override
    public void createUser(String adminUsername, String newUsername, String password, String role)
            throws RemoteException, AuctionException {
        // TODO: verify admin role, hash password, insert user
    }

    @Override
    public List<User> getAllUsers(String adminUsername) throws RemoteException, AuctionException {
        // TODO: verify admin role, return all users
        return Collections.emptyList();
    }

    @Override
    public byte[] backupDatabase(String adminUsername) throws RemoteException, AuctionException {
        // TODO: verify admin, return DB file bytes
        return new byte[0];
    }

    @Override
    public List<String> getAuditLogs(String adminUsername, int lastNLines)
            throws RemoteException, AuctionException {
        // TODO: verify admin, read last N lines from audit log
        return Collections.emptyList();
    }
}
