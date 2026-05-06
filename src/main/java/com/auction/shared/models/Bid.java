package com.auction.shared.models;

import java.io.Serializable;

/**
 * Records a single bid on an auction item. Serializable for RMI transport.
 * Timestamp is ISO-8601 String to avoid serialization issues.
 */
public class Bid implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int auctionItemId;
    private String bidderUsername;
    private double amount;
    private String timestamp; // ISO-8601

    public Bid() {}

    public Bid(int id, int auctionItemId, String bidderUsername, double amount, String timestamp) {
        this.id = id;
        this.auctionItemId = auctionItemId;
        this.bidderUsername = bidderUsername;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    // --- Getters ---
    public int getId() { return id; }
    public int getAuctionItemId() { return auctionItemId; }
    public String getBidderUsername() { return bidderUsername; }
    public double getAmount() { return amount; }
    public String getTimestamp() { return timestamp; }

    // --- Setters ---
    public void setId(int id) { this.id = id; }
    public void setAuctionItemId(int auctionItemId) { this.auctionItemId = auctionItemId; }
    public void setBidderUsername(String bidderUsername) { this.bidderUsername = bidderUsername; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "Bid{id=" + id + ", auction=" + auctionItemId
             + ", bidder='" + bidderUsername + "', amount=" + amount + "}";
    }
}
