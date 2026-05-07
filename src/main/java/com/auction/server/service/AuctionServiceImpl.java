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
        User u = userRepo.findUserByUsername(username);
        if (u != null) {
            String hash = com.auction.server.util.SecurityUtil.hashPassword(password);
            if (u.getPasswordHash().equals(hash)) {
                return u;
            }
        }
        return null;
    }

    // --- Auction Browsing ---
    @Override
    public List<AuctionItem> getActiveAuctions() throws RemoteException {
        return auctionRepo.findActiveAuctions();
    }

    @Override
    public AuctionItem getAuctionById(int auctionId) throws RemoteException {
        return auctionRepo.findAuctionById(auctionId);
    }

    // --- Bidding ---
    @Override
    public void placeBid(int auctionId, String bidderUsername, double amount,
                          double clientExpectedPrice) throws RemoteException, AuctionException {
        ReentrantLock lock = getLock(auctionId);
        lock.lock();
        try {
            AuctionItem item = auctionRepo.findAuctionById(auctionId);
            if (item == null) throw new AuctionException("Auction not found");
            if (!com.auction.shared.Constants.STATUS_ACTIVE.equals(item.getStatus())) {
                throw new com.auction.shared.exceptions.AuctionClosedException("Auction is closed");
            }
            if (bidderUsername.equals(item.getSellerUsername())) {
                throw new com.auction.shared.exceptions.SelfBidException("Cannot bid on your own auction");
            }
            if (bidderUsername.equals(item.getHighestBidderUsername())) {
                throw new com.auction.shared.exceptions.DuplicateBidException("You are already the highest bidder");
            }
            if (clientExpectedPrice != item.getCurrentBid()) {
                throw new com.auction.shared.exceptions.StaleDataException("Price has changed. Please refresh and try again.");
            }
            if (amount < item.getCurrentBid() * 1.05 && amount > 0) {
                // First bid is allowed to be exactly starting price or more, subsequent bids must be +5%
                if (!(item.getHighestBidderUsername() == null && amount >= item.getStartingPrice())) {
                    throw new com.auction.shared.exceptions.InsufficientBidException("Bid must be at least 5% higher than current bid");
                }
            }

            java.time.Instant now = java.time.Instant.now();
            java.time.Instant endTime = java.time.Instant.parse(item.getEndTime());
            if (now.isAfter(endTime)) {
                throw new com.auction.shared.exceptions.AuctionClosedException("Auction time has expired");
            }

            // Snipe protection
            if (java.time.Duration.between(now, endTime).getSeconds() < 30) {
                endTime = now.plusSeconds(30);
                auctionRepo.updateAuctionEndTime(auctionId, endTime.toString());
            }

            auctionRepo.updateAuctionBid(auctionId, amount, bidderUsername);
            
            Bid bid = new Bid();
            bid.setAuctionItemId(auctionId);
            bid.setBidderUsername(bidderUsername);
            bid.setAmount(amount);
            bid.setTimestamp(now.toString());
            bidRepo.insertBid(bid);
            
            // Log audit
            // We assume fileHandler is used to write audit logs later
            // fileHandler.appendAuditLog("...");
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<Bid> getBidHistory(int auctionId) throws RemoteException {
        return bidRepo.findBidsByAuctionId(auctionId);
    }

    // --- Auction Management ---
    @Override
    public int createAuction(AuctionItem item, byte[] image1, byte[] image2, byte[] image3)
            throws RemoteException {
        item.setStatus(com.auction.shared.Constants.STATUS_ACTIVE);
        int auctionId = auctionRepo.insertAuction(item);
        
        String p1 = null, p2 = null, p3 = null;
        if (image1 != null && image1.length > 0) p1 = imageManager.saveImage(auctionId, 1, image1);
        if (image2 != null && image2.length > 0) p2 = imageManager.saveImage(auctionId, 2, image2);
        if (image3 != null && image3.length > 0) p3 = imageManager.saveImage(auctionId, 3, image3);
        
        if (p1 != null || p2 != null || p3 != null) {
            auctionRepo.updateAuctionImages(auctionId, p1, p2, p3);
        }
        return auctionId;
    }

    @Override
    public void cancelAuction(int auctionId, String sellerUsername)
            throws RemoteException, AuctionException {
        ReentrantLock lock = getLock(auctionId);
        lock.lock();
        try {
            AuctionItem item = auctionRepo.findAuctionById(auctionId);
            if (item == null) throw new AuctionException("Auction not found");
            if (!item.getSellerUsername().equals(sellerUsername)) {
                throw new AuctionException("Only the seller can cancel this auction");
            }
            if (bidRepo.countBidsByAuctionId(auctionId) > 0) {
                throw new AuctionException("Cannot cancel auction with active bids");
            }
            auctionRepo.updateAuctionStatus(auctionId, com.auction.shared.Constants.STATUS_CANCELLED);
        } finally {
            lock.unlock();
        }
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
        User admin = userRepo.findUserByUsername(adminUsername);
        if (admin == null || !com.auction.shared.Constants.ADMIN.equals(admin.getRoleType())) {
            throw new AuctionException("Only admins can create users");
        }
        if (userRepo.findUserByUsername(newUsername) != null) {
            throw new AuctionException("Username already exists");
        }
        String hash = com.auction.server.util.SecurityUtil.hashPassword(password);
        userRepo.insertUser(newUsername, hash, role);
    }

    @Override
    public List<User> getAllUsers(String adminUsername) throws RemoteException, AuctionException {
        User admin = userRepo.findUserByUsername(adminUsername);
        if (admin == null || !com.auction.shared.Constants.ADMIN.equals(admin.getRoleType())) {
            throw new AuctionException("Only admins can view all users");
        }
        return userRepo.findAllUsers();
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
