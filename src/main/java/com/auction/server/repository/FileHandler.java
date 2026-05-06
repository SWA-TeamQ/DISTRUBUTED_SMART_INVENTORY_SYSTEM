package com.auction.server.repository;

import com.auction.shared.Constants;
import com.auction.shared.models.AuctionItem;

import java.util.List;

/**
 * File I/O handler for CSV exports and append-only audit logging.
 * Audit log format: [ISO-timestamp] [LEVEL] actor: description
 */
public class FileHandler {

    /**
     * Generate CSV content for a seller's auctions.
     * Columns: AuctionID,Title,Category,StartingPrice,FinalPrice,Winner,Status,StartTime,EndTime
     * @return CSV as byte[] (UTF-8 encoded)
     */
    public byte[] generateCSV(List<AuctionItem> auctions) {
        // TODO: build CSV string with header + rows
        return new byte[0];
    }

    /**
     * Append a line to the audit log.
     * @param level INFO, WARN, ERROR
     * @param actor username or "REAPER", "SYSTEM"
     * @param description what happened
     */
    public void appendAuditLog(String level, String actor, String description) {
        // TODO: append to Constants.AUDIT_LOG_PATH
    }

    /**
     * Read last N lines from audit log.
     * @return list of log lines, newest last
     */
    public List<String> readAuditLog(int lastNLines) {
        // TODO: read from Constants.AUDIT_LOG_PATH
        return List.of();
    }

    /**
     * Read the entire database file as bytes for backup.
     * @return DB file bytes
     */
    public byte[] readDatabaseFile() {
        // TODO: read Constants.DB_PATH as byte[]
        return new byte[0];
    }
}
