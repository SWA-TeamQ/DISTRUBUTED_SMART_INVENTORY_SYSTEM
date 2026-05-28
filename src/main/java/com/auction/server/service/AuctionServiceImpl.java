package com.auction.server.service;

import com.auction.shared.Constants;
import com.auction.shared.exceptions.*;
import com.auction.shared.interfaces.IAuctionService;
import com.auction.shared.models.*;
import com.auction.server.core.AdminManager;
import com.auction.server.core.AuctionManager;
import com.auction.server.core.ImageStore;
import com.auction.server.core.SessionContext;
import com.auction.server.core.logging.AsyncLogger;
import com.auction.server.core.logging.EventType;
import com.auction.server.core.logging.LogCategory;
import com.auction.server.repository.UserRepository;
import com.auction.server.util.SecurityUtil;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionServiceImpl extends UnicastRemoteObject implements IAuctionService {
    private static final long serialVersionUID = 1L;

    private final UserRepository userRepo;
    private final AuctionManager auctionManager;
    private final AdminManager adminManager;
    private final ImageStore imageStore;

    private static class SessionInfo {
        final SessionContext context;
        Instant expiresAt;

        SessionInfo(SessionContext context, Instant expiresAt) {
            this.context = context;
            this.expiresAt = expiresAt;
        }
    }

    private final ConcurrentHashMap<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    public AuctionServiceImpl(UserRepository userRepo,
                              AuctionManager auctionManager,
                              AdminManager adminManager,
                              ImageStore imageStore) throws RemoteException {
        super();
        this.userRepo = userRepo;
        this.auctionManager = auctionManager;
        this.adminManager = adminManager;
        this.imageStore = imageStore;
    }

    // --- Session Helpers ---

    private SessionContext validateSession(String token) throws AuctionException {
        if (token == null || token.isEmpty()) {
            throw new UnauthorizedException("Session token is missing");
        }
        SessionInfo sessionInfo = sessions.get(token);
        if (sessionInfo == null) {
            throw new UnauthorizedException("Invalid session token");
        }
        if (Instant.now().isAfter(sessionInfo.expiresAt)) {
            sessions.remove(token);
            throw new UnauthorizedException("Session token has expired");
        }
        sessionInfo.expiresAt = Instant.now().plus(Duration.ofMinutes(Constants.SESSION_TTL_MINUTES));
        return sessionInfo.context;
    }

    private SessionContext validateRole(String token, String... allowedRoles) throws AuctionException {
        SessionContext context = validateSession(token);
        boolean allowed = false;
        for (String role : allowedRoles) {
            if (role.equals(context.role())) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new UnauthorizedException("Access denied: insufficient permissions");
        }
        return context;
    }

    // --- Authentication ---

    @Override
    public String login(String username, String password) throws RemoteException, AuctionException {
        User u = userRepo.findUserByUsername(username);
        if (u != null) {
            String hash = SecurityUtil.hashPassword(password);
            if (u.getPasswordHash().equals(hash)) {
                String token = java.util.UUID.randomUUID().toString();
                SessionContext ctx = new SessionContext(username, u.getRoleType());
                sessions.put(token, new SessionInfo(ctx, Instant.now().plus(Duration.ofMinutes(Constants.SESSION_TTL_MINUTES))));
                AsyncLogger.log(LogCategory.SECURITY, EventType.LOGIN, "User=" + username);
                return token;
            }
        }
        AsyncLogger.log(LogCategory.SECURITY, EventType.LOGIN_FAILED, "Username=" + username);
        throw new AuctionException("Invalid username or password");
    }

    @Override
    public void register(String username, String password, String role) throws RemoteException, AuctionException {
        if (userRepo.findUserByUsername(username) != null) {
            throw new AuctionException("Username already exists");
        }
        String hash = SecurityUtil.hashPassword(password);
        userRepo.insertUser(username, hash, role);
        AsyncLogger.log(LogCategory.SECURITY, EventType.CREATE_AUCTION, "New User Registered: " + username + " Role=" + role);
    }

    @Override
    public String getMyRole(String token) throws RemoteException, AuctionException {
        return validateSession(token).role();
    }

    @Override
    public void logout(String token) throws RemoteException {
        SessionInfo sessionInfo = sessions.remove(token);
        if (sessionInfo != null) {
            AsyncLogger.log(LogCategory.SECURITY, EventType.LOGOUT, "User=" + sessionInfo.context.username());
        }
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
    public List<AuctionItem> getAllAuctions() throws RemoteException {
        return auctionManager.getAllAuctions();
    }

    @Override
    public List<AuctionItem> searchActiveAuctions(String query, String category, String sortBy) throws RemoteException {
        return auctionManager.searchActiveAuctions(query, category, sortBy);
    }

    @Override
    public List<AuctionItem> searchAllAuctions(String query, String category, String sortBy) throws RemoteException {
        return auctionManager.searchAllAuctions(query, category, sortBy);
    }

    @Override
    public List<AuctionItem> getActiveAuctionsBySeller(String sellerUsername, String token) throws RemoteException, AuctionException {
        validateSession(token);
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
        SessionContext context = validateRole(token, Constants.USER);

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
        SessionContext context = validateRole(token, Constants.USER);

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
    public void updateAuction(int auctionId, AuctionItem item, byte[] image1, byte[] image2, byte[] image3, String token)
            throws RemoteException, AuctionException {
        SessionContext context = validateRole(token, Constants.USER, Constants.ADMIN);

        String[] stagedPaths = null;
        try {
            stagedPaths = (image1 != null || image2 != null || image3 != null)
                ? imageStore.stageImages(image1, image2, image3)
                : null;
            auctionManager.updateAuction(auctionId, item, context, stagedPaths);
        } catch (AuctionException e) {
            imageStore.deleteStagedImages(stagedPaths);
            throw e;
        } catch (Exception e) {
            imageStore.deleteStagedImages(stagedPaths);
            throw new AuctionException("Internal error updating auction: " + e.getMessage());
        }
    }

    @Override
    public void cancelAuction(int auctionId, String token) throws RemoteException, AuctionException {
        SessionContext context = validateRole(token, Constants.USER, Constants.ADMIN);

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
        SessionContext context = validateRole(token, Constants.USER, Constants.ADMIN);

        try {
            auctionManager.relistAuction(auctionId, newEndTimeIso, context);
        } catch (AuctionException e) {
            throw e;
        } catch (Exception e) {
            throw new AuctionException("Internal error relisting auction: " + e.getMessage());
        }
    }

    @Override
    public void startAuction(int auctionId, String token) throws RemoteException, AuctionException {
        SessionContext context = validateRole(token, Constants.USER, Constants.ADMIN);
        try {
            auctionManager.startAuction(auctionId, context);
        } catch (AuctionException e) {
            throw e;
        } catch (Exception e) {
            throw new AuctionException("Internal error starting auction: " + e.getMessage());
        }
    }

    // --- Bidder Activity ---

    @Override
    public List<Bid> getMyBids(String token) throws RemoteException, AuctionException {
        SessionContext context = validateRole(token, Constants.USER);
        return auctionManager.findBidsByBidder(context.username());
    }

    @Override
    public List<AuctionItem> getMyWonAuctions(String token) throws RemoteException, AuctionException {
        SessionContext context = validateRole(token, Constants.USER);
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

    @Override
    public byte[] getFullImage(int auctionId, int imageIndex) throws RemoteException {
        AuctionItem item = auctionManager.getAuctionById(auctionId);
        if (item == null) return new byte[0];
        String path = resolveImagePath(item, imageIndex);
        return imageStore.loadFullImage(path);
    }

    private String resolveImagePath(AuctionItem item, int index) {
        if (index == 0) return item.getImg1();
        if (index == 1) return item.getImg2();
        if (index == 2) return item.getImg3();
        return null;
    }

    // --- Data Export ---

    @Override
    public byte[] exportAuctionsToCSV(String token) throws RemoteException, AuctionException {
        SessionContext context = validateSession(token);
        return adminManager.exportAuctionsToCSV(context);
    }

    // --- Administration ---

    @Override
    public void createUser(String newUsername, String password, String role, String token)
            throws RemoteException, AuctionException {
        SessionContext context = validateRole(token, Constants.ADMIN);
        adminManager.createUser(newUsername, password, role, context);
    }

    @Override
    public List<User> getAllUsers(String token) throws RemoteException, AuctionException {
        validateRole(token, Constants.ADMIN);
        return adminManager.getAllUsers();
    }

    @Override
    public byte[] backupDatabase(String token) throws RemoteException, AuctionException {
        SessionContext context = validateRole(token, Constants.ADMIN);
        byte[] backup = adminManager.backupDatabase();
        AsyncLogger.log(LogCategory.SYSTEM, EventType.DB_BACKUP,
                "User=" + context.username() + " Size=" + backup.length);
        return backup;
    }

    @Override
    public List<String> getAuditLogs(int lastNLines, String token)
            throws RemoteException, AuctionException {
        validateRole(token, Constants.ADMIN);
        return adminManager.getAuditLogs(lastNLines);
    }
}
