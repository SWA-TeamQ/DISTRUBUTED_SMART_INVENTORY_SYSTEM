package com.auction.client.util;

import com.auction.shared.models.AuctionItem;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

/**
 * Client-side CSV export helper that serializes the currently visible auction rows.
 */
public final class AuctionCsvExporter {

  private AuctionCsvExporter() {}

  public static String toCsv(Collection<AuctionItem> items) {
    StringBuilder builder = new StringBuilder();
    builder.append(
      "AuctionID,Title,Description,Category,StartingPriceCents,CurrentBidCents,HighestBidder,Seller,Status,StartTime,EndTime,CapEndTime\n"
    );

    if (items == null) {
      return builder.toString();
    }

    for (AuctionItem item : items) {
      if (item == null) {
        continue;
      }
      appendRow(builder, item);
    }

    return builder.toString();
  }

  public static void writeToFile(Collection<AuctionItem> items, Path output)
    throws IOException {
    Files.writeString(output, toCsv(items), StandardCharsets.UTF_8);
  }

  private static void appendRow(StringBuilder builder, AuctionItem item) {
    builder.append(item.getId()).append(',');
    builder.append(escape(item.getTitle())).append(',');
    builder.append(escape(item.getDescription())).append(',');
    builder.append(escape(item.getCategory())).append(',');
    builder.append(item.getStartingPriceCents()).append(',');
    builder.append(item.getCurrentBidCents()).append(',');
    builder.append(escape(item.getHighestBidderUsername())).append(',');
    builder.append(escape(item.getSellerUsername())).append(',');
    builder.append(escape(item.getStatus())).append(',');
    builder.append(escape(item.getStartTime())).append(',');
    builder.append(escape(item.getEndTime())).append(',');
    builder.append(escape(item.getCapEndTime())).append('\n');
  }

  private static String escape(String value) {
    if (value == null) {
      return "";
    }
    if (
      value.indexOf(',') >= 0 ||
      value.indexOf('"') >= 0 ||
      value.indexOf('\n') >= 0 ||
      value.indexOf('\r') >= 0
    ) {
      return '"' + value.replace("\"", "\"\"") + '"';
    }
    return value;
  }
}
