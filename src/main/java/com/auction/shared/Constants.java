package com.auction.shared;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Application-wide constants. Single source of truth for all magic values.
 */
public final class Constants {
    private Constants() {}

    // --- Roles ---
    public static final String ADMIN = "ADMIN";
    public static final String USER = "USER";

    // --- RMI ---
    public static final String RMI_SERVICE_NAME = "AuctionService";
    public static final int DEFAULT_RMI_PORT = 1999;
    public static final int SESSION_TTL_MINUTES = 30;

    // --- UDP Discovery ---
    public static final int UDP_BROADCAST_PORT = 9999;
    public static final String UDP_PREFIX = "RTDAS";
    public static final int UDP_BROADCAST_INTERVAL_MS = 2000;

    // --- Auction Rules ---
    public static final double MIN_BID_INCREMENT_PERCENT = 0.05;
    public static final int SNIPE_PROTECTION_SECONDS = 30;
    public static final int SNIPE_CAP_DEFAULT_MINUTES = 10;
    public static final int REAPER_INTERVAL_SECONDS = 1;

    // --- Polling ---
    public static final int CLIENT_POLL_INTERVAL_MS = 2000;

    // --- Images ---
    public static final int MAX_IMAGES_PER_AUCTION = 3;
    public static final long MAX_IMAGE_SIZE_BYTES = 2 * 1024 * 1024; // 2MB
    public static final int THUMBNAIL_SIZE = 40;

    // --- Paths ---
    public static final String DB_PATH = resolveProjectPath("data", "auction.db");
    public static final String IMAGES_DIR = resolveProjectPath("resources", "images");
    public static final String THUMBS_DIR = resolveProjectPath("resources", "thumbs");
    public static final String AUDIT_LOG_PATH = resolveProjectPath("logs", "audit.log");

    // --- Default Admin ---
    public static final String DEFAULT_ADMIN_USERNAME = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "admin";

    // --- Auction Status ---
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_SOLD = "SOLD";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    // --- Money Helper ---
    public static String formatCents(long cents) {
        return String.format("$%.2f", cents / 100.0);
    }

    private static String resolveProjectPath(String first, String... more) {
        try {
            Path codeSource = Paths.get(Constants.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath();
            Path root = codeSource;
            if (root.getFileName() != null && "classes".equalsIgnoreCase(root.getFileName().toString())) {
                root = root.getParent() != null ? root.getParent().getParent() : root;
            } else if (root.toString().endsWith(".jar")) {
                root = root.getParent();
            }
            if (root == null) {
                root = Paths.get("").toAbsolutePath();
            }
            return root.resolve(Paths.get(first, more)).toAbsolutePath().toString();
        } catch (URISyntaxException e) {
            return Paths.get("").toAbsolutePath().resolve(Paths.get(first, more)).toString();
        }
    }
}
