package com.auction.shared.models;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AuctionItem implements Serializable, Comparable<AuctionItem> {
    private int id;
    private String title;
    private String description;
    private double startingPrice;
    private double currentBid;
    private String highestBidder;
    private String sellerUsername;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean active;

    public AuctionItem() {}

    public AuctionItem(int id, String title, String description, double startingPrice,
                       String sellerUsername, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentBid = startingPrice;
        this.highestBidder = null;
        this.sellerUsername = sellerUsername;
        this.startTime = startTime;
        this.endTime = endTime;
        this.active = true;
    }

    @Override
    public int compareTo(AuctionItem other) {
        return Integer.compare(this.id, other.id);
    }

    // getters/setters omitted for brevity
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public double getStartingPrice() { return startingPrice; }
    public double getCurrentBid() { return currentBid; }
    public void setCurrentBid(double currentBid) { this.currentBid = currentBid; }
    public String getHighestBidder() { return highestBidder; }
    public void setHighestBidder(String highestBidder) { this.highestBidder = highestBidder; }
    public String getSellerUsername() { return sellerUsername; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
