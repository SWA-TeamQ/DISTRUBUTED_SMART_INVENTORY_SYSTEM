package com.auction.shared.models;

import java.io.Serializable;

/**
 * Represents an auction listing. Serializable for RMI transport.
 * Implements Comparable for sorting by ID.
 * All timestamps are ISO-8601 Strings to avoid serialization issues.
 */
public class AuctionItem implements Serializable, Comparable<AuctionItem> {
    private static final long serialVersionUID = 1L;

    private int id;
    private String title;
    private String description;
    private String category;          // from Category enum
    private long startingPriceCents;
    private long currentBidCents;
    private String highestBidderUsername;
    private String sellerUsername;
    private String startTime;         // ISO-8601 UTC
    private String endTime;           // ISO-8601 UTC
    private String capEndTime;        // ISO-8601 UTC (snipe cap)
    private String status;            // from AuctionStatus enum
    private String img1;              // filename or null
    private String img2;
    private String img3;
    private Integer relistedFrom;     // ID of parent auction if relisted, else null

    public AuctionItem() {}

    public AuctionItem(int id, String title, String description, String category,
                       long startingPriceCents, String sellerUsername,
                       String startTime, String endTime, String capEndTime) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.startingPriceCents = startingPriceCents;
        this.currentBidCents = startingPriceCents;
        this.highestBidderUsername = null;
        this.sellerUsername = sellerUsername;
        this.startTime = startTime;
        this.endTime = endTime;
        this.capEndTime = capEndTime;
        this.status = "ACTIVE";
        this.relistedFrom = null;
    }

    @Override
    public int compareTo(AuctionItem other) {
        return Integer.compare(this.id, other.id);
    }

    // --- Getters ---
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public long getStartingPriceCents() { return startingPriceCents; }
    public long getCurrentBidCents() { return currentBidCents; }
    public String getHighestBidderUsername() { return highestBidderUsername; }
    public String getSellerUsername() { return sellerUsername; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getCapEndTime() { return capEndTime; }
    public String getStatus() { return status; }
    public String getImg1() { return img1; }
    public String getImg2() { return img2; }
    public String getImg3() { return img3; }
    public Integer getRelistedFrom() { return relistedFrom; }

    // --- Setters ---
    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setStartingPriceCents(long startingPriceCents) { this.startingPriceCents = startingPriceCents; }
    public void setCurrentBidCents(long currentBidCents) { this.currentBidCents = currentBidCents; }
    public void setHighestBidderUsername(String u) { this.highestBidderUsername = u; }
    public void setSellerUsername(String sellerUsername) { this.sellerUsername = sellerUsername; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public void setCapEndTime(String capEndTime) { this.capEndTime = capEndTime; }
    public void setStatus(String status) { this.status = status; }
    public void setImg1(String img1) { this.img1 = img1; }
    public void setImg2(String img2) { this.img2 = img2; }
    public void setImg3(String img3) { this.img3 = img3; }
    public void setRelistedFrom(Integer relistedFrom) { this.relistedFrom = relistedFrom; }

    @Override
    public String toString() {
        return "AuctionItem{id=" + id + ", title='" + title + "', status=" + status + "}";
    }
}
