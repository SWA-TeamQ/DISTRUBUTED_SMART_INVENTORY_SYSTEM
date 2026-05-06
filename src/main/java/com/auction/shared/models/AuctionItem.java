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
    private double startingPrice;
    private double currentBid;
    private String highestBidderUsername;
    private String sellerUsername;
    private String startTime;         // ISO-8601
    private String endTime;           // ISO-8601
    private String status;            // from AuctionStatus enum
    private String img1;              // filename or null
    private String img2;
    private String img3;

    public AuctionItem() {}

    public AuctionItem(int id, String title, String description, String category,
                       double startingPrice, String sellerUsername,
                       String startTime, String endTime) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.startingPrice = startingPrice;
        this.currentBid = startingPrice;
        this.highestBidderUsername = null;
        this.sellerUsername = sellerUsername;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = "ACTIVE";
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
    public double getStartingPrice() { return startingPrice; }
    public double getCurrentBid() { return currentBid; }
    public String getHighestBidderUsername() { return highestBidderUsername; }
    public String getSellerUsername() { return sellerUsername; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getStatus() { return status; }
    public String getImg1() { return img1; }
    public String getImg2() { return img2; }
    public String getImg3() { return img3; }

    // --- Setters ---
    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }
    public void setCurrentBid(double currentBid) { this.currentBid = currentBid; }
    public void setHighestBidderUsername(String u) { this.highestBidderUsername = u; }
    public void setSellerUsername(String sellerUsername) { this.sellerUsername = sellerUsername; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public void setStatus(String status) { this.status = status; }
    public void setImg1(String img1) { this.img1 = img1; }
    public void setImg2(String img2) { this.img2 = img2; }
    public void setImg3(String img3) { this.img3 = img3; }

    @Override
    public String toString() {
        return "AuctionItem{id=" + id + ", title='" + title + "', status=" + status + "}";
    }
}
