package com.auction.server.util;

import com.auction.shared.models.AuctionItem;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class CsvExportUtil {
    private CsvExportUtil() {}

    public static byte[] generateAuctionsCsv(List<AuctionItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("AuctionID,Title,Category,StartingPrice,FinalPrice,Winner,Status,StartTime,EndTime\n");

        for (AuctionItem item : items) {
            sb.append(item.getId()).append(",");
            sb.append(escapeCsvField(item.getTitle())).append(",");
            sb.append(escapeCsvField(item.getCategory())).append(",");
            sb.append(item.getStartingPriceCents() / 100.0).append(",");
            sb.append(item.getCurrentBidCents() / 100.0).append(",");
            sb.append(escapeCsvField(item.getHighestBidderUsername() != null ? item.getHighestBidderUsername() : ""))
              .append(",");
            sb.append(escapeCsvField(item.getStatus())).append(",");
            sb.append(escapeCsvField(item.getStartTime())).append(",");
            sb.append(escapeCsvField(item.getEndTime())).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String escapeCsvField(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
