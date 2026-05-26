package com.auction.server.core;

import com.auction.server.repository.UserRepository;
import com.auction.shared.Constants;
import com.auction.shared.exceptions.AuctionException;
import com.auction.shared.exceptions.UnauthorizedException;
import com.auction.shared.models.AuctionItem;
import com.auction.shared.models.User;
import com.auction.server.core.logging.AsyncLogger;
import com.auction.server.core.logging.LogCategory;
import com.auction.server.core.logging.EventType;

import java.util.Collections;
import java.util.List;

public class AdminManager {
    private final AuctionManager auctionManager;
    private final UserRepository userRepo;

    public AdminManager(AuctionManager auctionManager, UserRepository userRepo) {
        this.auctionManager = auctionManager;
        this.userRepo = userRepo;
    }

    public byte[] exportAuctionsToCSV(SessionContext context) throws AuctionException {
        List<AuctionItem> items;
        if (Constants.ADMIN.equals(context.role())) {
            items = auctionManager.getActiveAuctions();
        } else if (Constants.USER.equals(context.role())) {
            items = auctionManager.findAuctionsBySeller(context.username());
        } else {
            throw new UnauthorizedException("Only users or admins can export auctions to CSV");
        }

        byte[] csvData = com.auction.server.util.CsvExportUtil.generateAuctionsCsv(items);
        AsyncLogger.log(LogCategory.SYSTEM, EventType.CSV_EXPORT,
                "User=" + context.username() + " Count=" + items.size());
        return csvData;
    }

    public List<String> getAuditLogs(int lastNLines, SessionContext context) throws AuctionException {
        try {
            return com.auction.server.util.AdminUtil.readAuditLogs(lastNLines);
        } catch (Exception e) {
            throw new AuctionException("Failed to read audit logs: " + e.getMessage());
        }
    }

    public List<User> getAllUsers(SessionContext context) {
        return userRepo.findAllUsers().stream()
            .map(u -> new User(u.getUsername(), null, u.getRoleType(), u.getCreatedAt()))
            .toList();
    }

    public List<User> searchUsers(String query, SessionContext context) {
        return userRepo.searchUsers(query).stream()
            .map(u -> new User(u.getUsername(), null, u.getRoleType(), u.getCreatedAt()))
            .toList();
    }

    public void promoteUserToAdmin(String username, SessionContext context) throws AuctionException {
        if (userRepo.findUserByUsername(username) == null) {
            throw new AuctionException("User not found");
        }
        userRepo.promoteUserToAdmin(username);
        AsyncLogger.log(LogCategory.SECURITY, EventType.LOGIN,
                "Admin=" + context.username() + " PromotedUser=" + username);
    }
}
