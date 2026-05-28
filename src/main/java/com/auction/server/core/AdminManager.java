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
import com.auction.server.util.CsvExportUtil;
import com.auction.server.util.AdminUtil;

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

        byte[] csvData = CsvExportUtil.generateAuctionsCsv(items);
        AsyncLogger.log(LogCategory.SYSTEM, EventType.CSV_EXPORT,
                "User=" + context.username() + " Count=" + items.size());
        return csvData;
    }

    public List<User> getAllUsers() {
        return userRepo.findAllUsers().stream()
            .map(u -> new User(u.getUsername(), null, u.getRoleType(), u.getCreatedAt()))
            .toList();
    }

    public void createUser(String newUsername, String password, String role, SessionContext context) throws AuctionException {
        if (userRepo.findUserByUsername(newUsername) != null) {
            throw new AuctionException("Username already exists");
        }
        String hash = com.auction.server.util.SecurityUtil.hashPassword(password);
        userRepo.insertUser(newUsername, hash, role);
        AsyncLogger.log(LogCategory.SECURITY, EventType.CREATE_AUCTION,
                "Admin=" + context.username() + " NewUser=" + newUsername + " Role=" + role);
    }

    public byte[] backupDatabase() throws AuctionException {
        java.io.File tempFile = new java.io.File("data/backup_" + java.util.UUID.randomUUID().toString() + ".db");
        try {
            String sql = "VACUUM INTO '" + tempFile.getAbsolutePath().replace("\\", "/") + "'";
            try (var stmt = userRepo.getConnection().createStatement()) {
                stmt.execute(sql);
            }
            byte[] bytes = java.nio.file.Files.readAllBytes(tempFile.toPath());
            return bytes;
        } catch (Exception e) {
            throw new AuctionException("Failed to backup database: " + e.getMessage());
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    public List<String> getAuditLogs(int lastNLines) throws AuctionException {
        try {
            return AdminUtil.readAuditLogs(lastNLines);
        } catch (Exception e) {
            throw new AuctionException("Failed to read audit logs: " + e.getMessage());
        }
    }
}
