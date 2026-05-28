package com.auction.shared.models;

import com.auction.shared.Constants;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Records a single bid on an auction item. Serializable for RMI transport.
 * Timestamp is ISO-8601 String to avoid serialization issues.
 */
public class Bid implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private int id;
    private int auctionItemId;
    private String bidderUsername;
    private long amountCents;
    private String timestamp; // ISO-8601 UTC

    public Bid() {}

    public Bid(int id, int auctionItemId, String bidderUsername, long amountCents, String timestamp) {
        this.id = id;
        this.auctionItemId = auctionItemId;
        this.bidderUsername = bidderUsername;
        this.amountCents = amountCents;
        this.timestamp = timestamp;
    }

    // --- Getters ---
    public int getId() { return id; }
    public int getAuctionItemId() { return auctionItemId; }
    public String getBidderUsername() { return bidderUsername; }
    public long getAmountCents() { return amountCents; }
    public String getAmountFormatted() { return Constants.formatCents(amountCents); }
    public String getTimestamp() { return timestamp; }
    public String getTimestampFormatted() {
        if (timestamp == null || timestamp.isBlank()) {
            return "";
        }
        try {
            Instant instant = Instant.parse(timestamp);
            ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
            return zonedDateTime.format(TIMESTAMP_FORMATTER);
        } catch (Exception ignored) {
            return timestamp;
        }
    }

    // --- Setters ---
    public void setId(int id) { this.id = id; }
    public void setAuctionItemId(int auctionItemId) { this.auctionItemId = auctionItemId; }
    public void setBidderUsername(String bidderUsername) { this.bidderUsername = bidderUsername; }
    public void setAmountCents(long amountCents) { this.amountCents = amountCents; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "Bid{id=" + id + ", auction=" + auctionItemId
             + ", bidder='" + bidderUsername + "', amountCents=" + amountCents + "}";
    }
}
