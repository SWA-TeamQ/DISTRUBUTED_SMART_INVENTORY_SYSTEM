package com.auction.server.service;

import com.auction.shared.exceptions.AuctionException;
import com.auction.shared.interfaces.IAuctionService;
import com.auction.shared.models.AuctionItem;
import com.auction.shared.models.Bid;
import com.auction.shared.models.User;
import com.auction.server.core.AuctionManager;
import com.auction.server.core.LifecycleManager;
import com.auction.server.core.ImageStore;
import com.auction.server.repository.UserRepository;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.List;


/**
 * RMI service implementation. Extends UnicastRemoteObject for automatic RMI export.
 * Uses per-auction ReentrantLock for concurrency control on placeBid.
 */
public class AuctionServiceImpl extends UnicastRemoteObject implements IAuctionService {
    private static final long serialVersionUID = 1L;

    private final AuctionManager auctionManager;
    private final LifecycleManager lifecycleManager;
    private final ImageStore imageStore;
    private final UserRepository userRepo; // Kept for auth

    public AuctionServiceImpl(UserRepository userRepo, AuctionManager auctionManager,
                              LifecycleManager lifecycleManager, ImageStore imageStore) throws RemoteException {
        super();
        this.userRepo = userRepo;
        this.auctionManager = auctionManager;
        this.lifecycleManager = lifecycleManager;
        this.imageStore = imageStore;
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
        // Delegate to repo through manager or directly if simple read
        // For architectural purity, we could add a read interface to AuctionManager, 
        // but since it's a simple find, direct repo access in service is acceptable 
        // as long as no logic is duplicated.
        // However, let's keep it clean.
        return auctionManager.getActiveAuctions();
    }

    @Override
    public AuctionItem getAuctionById(int auctionId) throws RemoteException {
        return auctionManager.getAuctionById(auctionId);
    }

    // --- Bidding ---
    @Override
    public void placeBid(int auctionId, String bidderUsername, double amount,
                          double clientExpectedPrice) throws RemoteException, AuctionException {
        auctionManager.placeBid(auctionId, bidderUsername, amount, clientExpectedPrice);
    }

    @Override
    public List<Bid> getBidHistory(int auctionId) throws RemoteException {
        return auctionManager.getBidHistory(auctionId);
    }

    // --- Auction Management ---
    @Override
    public int createAuction(AuctionItem item, byte[] i1, byte[] i2, byte[] i3) throws RemoteException {
        int auctionId = auctionManager.createAuction(item);
        imageStore.saveAuctionImages(auctionId, i1, i2, i3);
        return auctionId;
    }

    @Override
    public void cancelAuction(int auctionId, String sellerUsername) throws RemoteException, AuctionException {
        auctionManager.cancelAuction(auctionId, sellerUsername);
    }

    // --- Image Handling ---
    @Override
    public byte[] getThumbnail(int auctionId, int imageIndex) throws RemoteException {
        return imageStore.loadThumbnail(auctionId);
    }

    @Override
    public byte[] getFullImage(int auctionId, int imageIndex) throws RemoteException {
        AuctionItem item = auctionManager.getAuctionById(auctionId);
        String path = null;
        if (imageIndex == 1) path = item.getImg1();
        else if (imageIndex == 2) path = item.getImg2();
        else if (imageIndex == 3) path = item.getImg3();
        return imageStore.loadFullImage(path);
    }

    // --- Data Export ---
    @Override
    public byte[] exportAuctionsToCSV(String sellerUsername) throws RemoteException {
        // TODO: This should probably go to a ReportingManager deep module
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
        // TODO: delegate to a SystemManager
        return new byte[0];
    }

    @Override
    public List<String> getAuditLogs(String adminUsername, int lastNLines)
            throws RemoteException, AuctionException {
        // TODO: delegate to SystemManager/AuditLog
        return Collections.emptyList();
    }
}
