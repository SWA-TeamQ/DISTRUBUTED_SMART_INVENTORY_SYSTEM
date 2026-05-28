package com.auction.server.service;

import com.auction.shared.Constants;
import com.auction.shared.exceptions.*;
import com.auction.shared.interfaces.IAuctionService;
import com.auction.shared.models.*;
import com.auction.server.core.AuctionManager;
import com.auction.server.core.SessionManager;
import com.auction.server.core.AdminManager;
import com.auction.server.core.ImageStore;
import com.auction.server.core.SessionContext;
import com.auction.server.core.logging.AsyncLogger;
import com.auction.server.core.logging.LogCategory;
import com.auction.server.core.logging.EventType;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.Instant;
import java.util.List;

public class AuctionServiceImpl extends UnicastRemoteObject implements IAuctionService {
    private static final long serialVersionUID = 1L;

    private final AuctionManager auctionManager;
    private final SessionManager sessionManager;
    private final AdminManager adminManager;
    private final ImageStore imageStore;

    public AuctionServiceImpl(AuctionManager auctionManager,
                              SessionManager sessionManager,
                              AdminManager adminManager,
                              ImageStore imageStore) throws RemoteException {
        super();
        this.auctionManager = auctionManager;
        this.sessionManager = sessionManager;
        this.adminManager = adminManager;
        this.imageStore = imageStore;
    }

    // --- Authentication ---
    @Override
    public String login(String username, String password) throws RemoteException, AuctionException {
        return sessionManager.login(username, password);
    }

    @Override
    public void register(String username, String password, String role) throws RemoteException, AuctionException {
        sessionManager.register(username, password, role);
    }

    @Override
    public String getMyRole(String token) throws RemoteException, AuctionException {
        return sessionManager.validateSession(token).role();
    }

    @Override
    public void logout(String token) throws RemoteException {
        sessionManager.logout(token);
    }

    @Override
    public String serverTime() throws RemoteException {
        return Instant.now().toString();
    }

    // --- Auction Browsing ---
    @Override
    public List<AuctionItem> getActiveAuctions() throws RemoteException {
        return auctionManager.getActiveAuctions();
    }

    @Override
    public List<AuctionItem> getActiveAuctionsBySeller(String sellerUsername, String token) throws RemoteException, AuctionException {
        sessionManager.validateSession(token);
        return auctionManager.findActiveAuctionsBySeller(sellerUsername);
    }

    @Override
    public List<AuctionItem> getAuctionsBySeller(String sellerUsername, String token) throws RemoteException, AuctionException {
        sessionManager.validateSession(token);
        return auctionManager.findAuctionsBySeller(sellerUsername);
    }

    @Override
    public AuctionItem getAuctionById(int auctionId) throws RemoteException {
        return auctionManager.getAuctionById(auctionId);
    }

    // --- Bidding ---
    @Override
    public void placeBid(int auctionId, long amountCents, long clientExpectedPriceCents, String token)
            throws RemoteException, AuctionException {
        SessionContext context = sessionManager.validateRole(token, Constants.USER);
        try {
            auctionManager.placeBid(auctionId, context, amountCents, clientExpectedPriceCents);
        } catch (AuctionException e) {
            throw e;
        } catch (Exception e) {
            throw new AuctionException("Internal error placing bid: " + e.getMessage());
        }
    }

    @Override
    public List<Bid> getBidHistory(int auctionId) throws RemoteException {
        return auctionManager.getBidHistory(auctionId);
    }

    // --- Auction Management (Seller) ---
    @Override
    public int createAuction(AuctionItem item, byte[] image1, byte[] image2, byte[] image3, String token)
            throws RemoteException, AuctionException {
        SessionContext context = sessionManager.validateRole(token, Constants.USER);
        String[] stagedPaths = null;
        try {
            stagedPaths = imageStore.stageImages(image1, image2, image3);
            return auctionManager.createAuction(item, context, stagedPaths);
        } catch (AuctionException e) {
            imageStore.deleteStagedImages(stagedPaths);
            throw e;
        } catch (Exception e) {
            imageStore.deleteStagedImages(stagedPaths);
            throw new AuctionException("Internal error creating auction: " + e.getMessage());
        }
    }

    @Override
    public void cancelAuction(int auctionId, String token) throws RemoteException, AuctionException {
        SessionContext context = sessionManager.validateRole(token, Constants.USER, Constants.ADMIN);
        try {
            auctionManager.cancelAuction(auctionId, context);
        } catch (AuctionException e) {
            throw e;
        } catch (Exception e) {
            throw new AuctionException("Internal error cancelling auction: " + e.getMessage());
        }
    }

    @Override
    public void relistAuction(int auctionId, String newEndTimeIso, String token)
            throws RemoteException, AuctionException {
        SessionContext context = sessionManager.validateRole(token, Constants.USER, Constants.ADMIN);
        try {
            auctionManager.relistAuction(auctionId, newEndTimeIso, context);
        } catch (AuctionException e) {
            throw e;
        } catch (Exception e) {
            throw new AuctionException("Internal error relisting auction: " + e.getMessage());
        }
    }

    // --- Bidder Activity ---
    @Override
    public List<Bid> getMyBids(String token) throws RemoteException, AuctionException {
        SessionContext context = sessionManager.validateRole(token, Constants.USER);
        return auctionManager.findBidsByBidder(context.username());
    }

    @Override
    public List<AuctionItem> getMyWonAuctions(String token) throws RemoteException, AuctionException {
        SessionContext context = sessionManager.validateRole(token, Constants.USER);
        return auctionManager.findWonAuctionsByBidder(context.username());
    }

    // --- Image Handling (LQIP) ---
    @Override
    public byte[] getThumbnail(int auctionId, int imageIndex) throws RemoteException {
        AuctionItem item = auctionManager.getAuctionById(auctionId);
        if (item == null) return new byte[0];
        String path = resolveImagePath(item, imageIndex);
        return imageStore.loadThumbnail(path);
    }

    private String resolveImagePath(AuctionItem item, int index) {
        return switch (index) {
            case 0 -> item.getImg1();
            case 1 -> item.getImg2();
            case 2 -> item.getImg3();
            default -> null;
        };
    }

    @Override
    public byte[] getFullImage(int auctionId, int imageIndex) throws RemoteException {
        AuctionItem item = auctionManager.getAuctionById(auctionId);
        if (item == null) return new byte[0];
        String path = resolveImagePath(item, imageIndex);
        return imageStore.loadFullImage(path);
    }

    // --- Data Export ---
    @Override
    public byte[] exportAuctionsToCSV(String token) throws RemoteException, AuctionException {
        SessionContext context = sessionManager.validateSession(token);
        return adminManager.exportAuctionsToCSV(context);
    }

    // --- Administration ---
    @Override
    public List<User> getAllUsers(String token) throws RemoteException, AuctionException {
        SessionContext context = sessionManager.validateRole(token, Constants.ADMIN);
        return adminManager.getAllUsers(context);
    }

    @Override
    public List<User> searchUsers(String query, String token) throws RemoteException, AuctionException {
        SessionContext context = sessionManager.validateRole(token, Constants.ADMIN);
        return adminManager.searchUsers(query, context);
    }

    @Override
    public void promoteUserToAdmin(String username, String token) throws RemoteException, AuctionException {
        SessionContext context = sessionManager.validateRole(token, Constants.ADMIN);
        adminManager.promoteUserToAdmin(username, context);
    }

    @Override
    public void demoteUserToStandard(String username, String token) throws RemoteException, AuctionException {
        SessionContext context = sessionManager.validateRole(token, Constants.ADMIN);
        adminManager.demoteUserToStandard(username, context);
    }

    @Override
    public List<String> getAuditLogs(int lastNLines, String token) throws RemoteException, AuctionException {
        SessionContext context = sessionManager.validateRole(token, Constants.ADMIN);
        return adminManager.getAuditLogs(lastNLines, context);
    }
}
