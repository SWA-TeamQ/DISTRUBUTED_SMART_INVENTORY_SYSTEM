package com.auction.server.core;

import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.BidRepository;
import com.auction.shared.Constants;
import com.auction.shared.exceptions.*;
import com.auction.shared.models.AuctionItem;
import com.auction.shared.models.Bid;

import java.time.Duration;
import java.time.Instant;

/**
 * Deep module owning all bidding invariants and auction mutations.
 * Provides high-leverage interface for domain actions.
 */
public class AuctionManager {
    private final AuctionRepository auctionRepo;
    private final BidRepository bidRepo;

    public AuctionManager(AuctionRepository auctionRepo, BidRepository bidRepo) {
        this.auctionRepo = auctionRepo;
        this.bidRepo = bidRepo;
    }

    public java.util.List<AuctionItem> getActiveAuctions() {
        return auctionRepo.findActiveAuctions();
    }

    public AuctionItem getAuctionById(int id) {
        return auctionRepo.findAuctionById(id);
    }

    public java.util.List<Bid> getBidHistory(int auctionId) {
        return bidRepo.findBidsByAuctionId(auctionId);
    }


    /**
     * Validates and places a bid.
     * Handles 5% rule, self-bid prevention, stale data, and snipe protection.
     */
    public void placeBid(int auctionId, String bidderUsername, double amount, double clientExpectedPrice) 
            throws AuctionException {
        
        AuctionItem item = auctionRepo.findAuctionById(auctionId);
        if (item == null) throw new AuctionException("Auction not found");

        validateActive(item);
        validateNotSeller(item, bidderUsername);
        validateNotCurrentWinner(item, bidderUsername);
        validateFreshness(item, clientExpectedPrice);
        validateMinimumBid(item, amount);
        validateNotExpired(item);

        applySnipeProtection(item);
        
        // Persist
        auctionRepo.updateAuctionBid(auctionId, amount, bidderUsername);
        
        Bid bid = new Bid();
        bid.setAuctionItemId(auctionId);
        bid.setBidderUsername(bidderUsername);
        bid.setAmount(amount);
        bid.setTimestamp(Instant.now().toString());
        bidRepo.insertBid(bid);
    }

    /**
     * Creates a new auction in ACTIVE status.
     */
    public int createAuction(AuctionItem item) {
        item.setStatus(Constants.STATUS_ACTIVE);
        return auctionRepo.insertAuction(item);
    }

    /**
     * Cancels an auction if no bids exist.
     */
    public void cancelAuction(int auctionId, String sellerUsername) throws AuctionException {
        AuctionItem item = auctionRepo.findAuctionById(auctionId);
        if (item == null) throw new AuctionException("Auction not found");
        
        if (!item.getSellerUsername().equals(sellerUsername)) {
            throw new AuctionException("Only the seller can cancel this auction");
        }
        
        if (bidRepo.countBidsByAuctionId(auctionId) > 0) {
            throw new AuctionException("Cannot cancel auction with active bids");
        }
        
        auctionRepo.updateAuctionStatus(auctionId, Constants.STATUS_CANCELLED);
    }

    private void validateActive(AuctionItem item) throws AuctionClosedException {
        if (!Constants.STATUS_ACTIVE.equals(item.getStatus())) {
            throw new AuctionClosedException("Auction is closed");
        }
    }

    private void validateNotSeller(AuctionItem item, String bidder) throws SelfBidException {
        if (bidder.equals(item.getSellerUsername())) {
            throw new SelfBidException("Cannot bid on your own auction");
        }
    }

    private void validateNotCurrentWinner(AuctionItem item, String bidder) throws DuplicateBidException {
        if (bidder.equals(item.getHighestBidderUsername())) {
            throw new DuplicateBidException("You are already the highest bidder");
        }
    }

    private void validateFreshness(AuctionItem item, double expected) throws StaleDataException {
        if (expected != item.getCurrentBid()) {
            throw new StaleDataException("Price has changed. Please refresh and try again.");
        }
    }

    private void validateMinimumBid(AuctionItem item, double amount) throws InsufficientBidException {
        if (amount < item.getCurrentBid() * 1.05 && amount > 0) {
            // First bid allowed to be >= starting price
            if (!(item.getHighestBidderUsername() == null && amount >= item.getStartingPrice())) {
                throw new InsufficientBidException("Bid must be at least 5% higher than current bid");
            }
        }
    }

    private void validateNotExpired(AuctionItem item) throws AuctionClosedException {
        Instant now = Instant.now();
        Instant endTime = Instant.parse(item.getEndTime());
        if (now.isAfter(endTime)) {
            throw new AuctionClosedException("Auction time has expired");
        }
    }

    private void applySnipeProtection(AuctionItem item) {
        Instant now = Instant.now();
        Instant endTime = Instant.parse(item.getEndTime());
        if (Duration.between(now, endTime).getSeconds() < 30) {
            Instant newEndTime = now.plusSeconds(30);
            auctionRepo.updateAuctionEndTime(item.getId(), newEndTime.toString());
        }
    }
}
