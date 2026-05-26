package com.auction.server.util;

import com.auction.shared.Constants;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

/**
 * Utility for administrative helper functions like log reading.
 */
public final class AdminUtil {
    private AdminUtil() {}

    /**
     * Reads the last N lines from the audit log file.
     */
    public static List<String> readAuditLogs(int lastNLines) throws IOException {
        File file = new File(Constants.AUDIT_LOG_PATH);
        if (!file.exists()) {
            return Collections.emptyList();
        }
        
        List<String> lines = Files.readAllLines(file.toPath());
        if (lines.size() <= lastNLines) {
            return lines;
        }
        return lines.subList(lines.size() - lastNLines, lines.size());
    }
}
