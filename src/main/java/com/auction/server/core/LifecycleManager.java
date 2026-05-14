package com.auction.server.core;

import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.BidRepository;
import com.auction.shared.Constants;
import com.auction.shared.models.AuctionItem;

import java.time.Instant;
import java.util.List;

/**
 * Deep module owning auction state transitions and background sweeping.
 * Concentrates logic for auction termination and lifecycle events.
 */
public class LifecycleManager {
    private final AuctionRepository auctionRepo;
    private final BidRepository bidRepo;

    public LifecycleManager(AuctionRepository auctionRepo, BidRepository bidRepo) {
        this.auctionRepo = auctionRepo;
        this.bidRepo = bidRepo;
    }

    /**
     * Scans for ACTIVE auctions that have passed their end time and 
     * transitions them to SOLD or EXPIRED based on bid presence.
     */
    public void sweepOverdue() {
        List<AuctionItem> active = auctionRepo.findActiveAuctions();
        Instant now = Instant.now();

        for (AuctionItem item : active) {
            try {
                Instant endTime = Instant.parse(item.getEndTime());
                if (now.isAfter(endTime)) {
                    finalizeAuction(item);
                }
            } catch (Exception e) {
                System.err.println("Failed to sweep auction " + item.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Transitions an auction to its final state.
     */
    private void finalizeAuction(AuctionItem item) {
        int bidCount = bidRepo.countBidsByAuctionId(item.getId());
        String finalStatus = (bidCount > 0) ? Constants.STATUS_SOLD : Constants.STATUS_EXPIRED;
        
        auctionRepo.updateAuctionStatus(item.getId(), finalStatus);
        
        // Potential extension point: Notify highest bidder/seller via future notification module
    }
}
