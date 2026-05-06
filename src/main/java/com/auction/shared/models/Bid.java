package com.auction.shared.models;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Bid implements Serializable {
    private int id;
    private int auctionItemId;
    private String bidderUsername;
    private double amount;
    private LocalDateTime timestamp;

    public Bid() {}

    public Bid(int id, int auctionItemId, String bidderUsername, double amount, LocalDateTime timestamp) {
        this.id = id;
        this.auctionItemId = auctionItemId;
        this.bidderUsername = bidderUsername;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public int getAuctionItemId() { return auctionItemId; }
    public String getBidderUsername() { return bidderUsername; }
    public double getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
