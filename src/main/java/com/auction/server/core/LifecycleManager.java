package com.auction.server.core;

import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.BidRepository;
import com.auction.shared.Constants;
import com.auction.shared.models.AuctionItem;
import com.auction.server.core.logging.AsyncLogger;
import com.auction.server.core.logging.LogCategory;
import com.auction.server.core.logging.EventType;

import java.time.Instant;
import java.util.List;

public class LifecycleManager {
    private final AuctionRepository auctionRepo;
    private final BidRepository bidRepo;
    private final LockManager lockManager;
    private final TransactionManager txManager;

    public LifecycleManager(AuctionRepository auctionRepo, BidRepository bidRepo, 
                            LockManager lockManager, TransactionManager txManager) {
        this.auctionRepo = auctionRepo;
        this.bidRepo = bidRepo;
        this.lockManager = lockManager;
        this.txManager = txManager;
    }

    public void sweepOverdue() {
        String nowTimeIso = Instant.now().toString();
        List<AuctionItem> overdueItems = auctionRepo.findActiveExpiredAuctions(nowTimeIso);
        
        for (AuctionItem item : overdueItems) {
            int auctionId = item.getId();
            lockManager.lock(auctionId);
            try {
                txManager.executeWithoutResult(() -> {
                    // Re-check status inside lock to prevent race condition
                    AuctionItem currentItem = auctionRepo.findAuctionById(auctionId);
                    if (currentItem != null && Constants.STATUS_ACTIVE.equals(currentItem.getStatus())) {
                        
                        if (Instant.now().isAfter(Instant.parse(currentItem.getEndTime()))) {
                            int bidCount = bidRepo.countBidsByAuctionId(auctionId);
                            if (bidCount > 0) {
                                auctionRepo.updateAuctionStatus(auctionId, Constants.STATUS_SOLD);
                                AsyncLogger.log(LogCategory.SYSTEM, EventType.AUCTION_SOLD, 
                                    "Auction=" + auctionId + " Winner=" + currentItem.getHighestBidderUsername() + " Amount=" + currentItem.getCurrentBidCents());
                            } else {
                                auctionRepo.updateAuctionStatus(auctionId, Constants.STATUS_EXPIRED);
                                AsyncLogger.log(LogCategory.SYSTEM, EventType.AUCTION_EXPIRED, 
                                    "Auction=" + auctionId + " Status=EXPIRED");
                            }
                        }
                    }
                });
            } catch (Exception e) {
                System.err.println("Error sweeping auction " + auctionId + ": " + e.getMessage());
            } finally {
                lockManager.unlock(auctionId);
            }
        }
    }
}
