package com.auction.server.util;

import com.auction.shared.Constants;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * Thread-safe audit logger writing append-only logs to data/logs/audit.log.
 */
public final class AuditLogger {
    private AuditLogger() {}

    public static synchronized void log(String action, String details) {
        String logLine = String.format("[%s] Action: %s | Details: %s\n", Instant.now().toString(), action, details);
        
        try {
            File logFile = new File(Constants.AUDIT_LOG_PATH);
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            Files.writeString(logFile.toPath(), logLine, 
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            System.err.println("Failed to write to audit log: " + e.getMessage());
        }
    }
}
