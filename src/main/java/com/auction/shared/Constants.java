package com.auction.shared;

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
    public static final int DEFAULT_RMI_PORT = 1099;
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
    public static final String DB_PATH = "data/auction.db";
    public static final String IMAGES_DIR = "resources/images";
    public static final String THUMBS_DIR = "resources/thumbs";
    public static final String AUDIT_LOG_PATH = "logs/audit.log";

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
}
