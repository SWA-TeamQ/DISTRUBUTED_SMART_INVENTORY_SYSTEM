package com.auction.server.core;

import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.BidRepository;
import com.auction.shared.Constants;
import com.auction.shared.models.AuctionItem;
import com.auction.server.core.logging.AsyncLogger;
import com.auction.server.core.logging.LogCategory;
import com.auction.server.core.logging.EventType;

import java.time.Instant;
import java.time.Duration;
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

    public void activateScheduled() {
        String nowTimeIso = Instant.now().toString();

        // Manual auctions get a 5-minute grace window; after that they are cancelled
        // unless the seller updates the start time.
        String manualCutoffIso = Instant.now().minus(Duration.ofMinutes(5)).toString();
        List<AuctionItem> overdueManual = auctionRepo.findManualScheduledAuctionsOverdue(manualCutoffIso);
        for (AuctionItem item : overdueManual) {
            int auctionId = item.getId();
            lockManager.lock(auctionId);
            try {
                txManager.executeWithoutResult(() -> {
                    AuctionItem current = auctionRepo.findAuctionById(auctionId);
                    if (current != null
                        && Constants.STATUS_SCHEDULED.equals(current.getStatus())
                        && Constants.START_MODE_MANUAL.equalsIgnoreCase(current.getStartMode())
                        && current.getStartTime() != null
                        && Instant.now().isAfter(Instant.parse(current.getStartTime()).plus(Duration.ofMinutes(5)))) {
                        auctionRepo.updateAuctionStatus(auctionId, Constants.STATUS_CANCELLED);
                        AsyncLogger.log(LogCategory.SYSTEM, EventType.CANCEL_AUCTION,
                                "Auction=" + auctionId + " Reason=manual-start-time-expired");
                    }
                });
            } catch (Exception e) {
                System.err.println("Error cancelling overdue manual auction " + auctionId + ": " + e.getMessage());
            } finally {
                lockManager.unlock(auctionId);
            }
        }

        List<AuctionItem> scheduled = auctionRepo.findScheduledAuctionsToStart(nowTimeIso);
        for (AuctionItem item : scheduled) {
            int auctionId = item.getId();
            lockManager.lock(auctionId);
            try {
                txManager.executeWithoutResult(() -> {
                    AuctionItem current = auctionRepo.findAuctionById(auctionId);
                    if (current != null && Constants.STATUS_SCHEDULED.equals(current.getStatus())) {
                        // Only activate if start_time has arrived
                        if (Instant.now().isAfter(Instant.parse(current.getStartTime())) || Instant.now().equals(Instant.parse(current.getStartTime()))) {
                            // ensure capEndTime exists
                            if (current.getEndTime() != null) {
                                Instant cap = Instant.parse(current.getEndTime()).plus(java.time.Duration.ofMinutes(Constants.SNIPE_CAP_DEFAULT_MINUTES));
                                current.setCapEndTime(cap.toString());
                            }
                            auctionRepo.updateAuctionStatus(auctionId, Constants.STATUS_ACTIVE);
                            AsyncLogger.log(com.auction.server.core.logging.LogCategory.SYSTEM, com.auction.server.core.logging.EventType.AUCTION_SCHEDULED_STARTED,
                                    "Auction=" + auctionId + " started automatically at " + nowTimeIso);
                        }
                    }
                });
            } catch (Exception e) {
                System.err.println("Error activating scheduled auction " + auctionId + ": " + e.getMessage());
            } finally {
                lockManager.unlock(auctionId);
            }
        }
    }
}
