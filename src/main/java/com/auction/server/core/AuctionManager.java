package com.auction.server.core;

import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.BidRepository;
import com.auction.shared.Constants;
import com.auction.shared.exceptions.*;
import com.auction.shared.models.AuctionItem;
import com.auction.shared.models.Bid;
import com.auction.server.core.logging.AsyncLogger;
import com.auction.server.core.logging.LogCategory;
import com.auction.server.core.logging.EventType;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class AuctionManager {
    private final AuctionRepository auctionRepo;
    private final BidRepository bidRepo;
    private final LockManager lockManager;
    private final TransactionManager txManager;

    public AuctionManager(AuctionRepository auctionRepo, BidRepository bidRepo, 
                          LockManager lockManager, TransactionManager txManager) {
        this.auctionRepo = auctionRepo;
        this.bidRepo = bidRepo;
        this.lockManager = lockManager;
        this.txManager = txManager;
    }

    public List<AuctionItem> getActiveAuctions() {
        return auctionRepo.findActiveAuctions();
    }

    public AuctionItem getAuctionById(int id) {
        return auctionRepo.findAuctionById(id);
    }

    public List<AuctionItem> findAuctionsBySeller(String sellerUsername) {
        return auctionRepo.findAuctionsBySeller(sellerUsername);
    }

    public List<Bid> getBidHistory(int auctionId) {
        return bidRepo.findBidsByAuctionId(auctionId);
    }

    public List<Bid> findBidsByBidder(String bidderUsername) {
        return bidRepo.findBidsByBidder(bidderUsername);
    }

    public List<AuctionItem> findWonAuctionsByBidder(String bidderUsername) {
        return auctionRepo.findWonAuctionsByBidder(bidderUsername);
    }

    public void placeBid(int auctionId, SessionContext user, long amountCents, long clientExpectedPriceCents) throws Exception {
        lockManager.lock(auctionId);
        try {
            txManager.executeWithoutResult(() -> {
                AuctionItem item = auctionRepo.findAuctionById(auctionId);
                if (item == null) throw new AuctionException("Auction not found");

                validateActive(item);
                validateNotSeller(item, user.username());
                validateNotCurrentWinner(item, user.username());
                validateFreshness(item, clientExpectedPriceCents);
                validateMinimumBid(item, amountCents);
                validateNotExpired(item);

                Instant now = Instant.now();
                String newEndTime = applySnipeProtection(item, now);

                Bid bid = new Bid();
                bid.setAuctionItemId(auctionId);
                bid.setBidderUsername(user.username());
                bid.setAmountCents(amountCents);
                bid.setTimestamp(now.toString());

                bidRepo.insertBid(bid);
                auctionRepo.updateAuctionBid(auctionId, amountCents, user.username());
                if (!newEndTime.equals(item.getEndTime())) {
                    auctionRepo.updateAuctionEndTime(auctionId, newEndTime);
                }

                AsyncLogger.log(LogCategory.BID, EventType.PLACE_BID, 
                    "User=" + user.username() + " Auction=" + auctionId + " Amount=" + amountCents);
            });
        } finally {
            lockManager.unlock(auctionId);
        }
    }

    public int createAuction(AuctionItem item, SessionContext user, String[] imagePaths) throws Exception {
        item.setSellerUsername(user.username());
        item.setStatus(Constants.STATUS_ACTIVE);
        if (item.getEndTime() != null) {
            Instant endTime = Instant.parse(item.getEndTime());
            Instant capEndTime = endTime.plus(Duration.ofMinutes(Constants.SNIPE_CAP_DEFAULT_MINUTES));
            item.setCapEndTime(capEndTime.toString());
        }
        if (imagePaths != null && imagePaths.length == 3) {
            item.setImg1(imagePaths[0]);
            item.setImg2(imagePaths[1]);
            item.setImg3(imagePaths[2]);
        }

        return txManager.execute(() -> {
            int auctionId = auctionRepo.insertAuction(item);
            AsyncLogger.log(LogCategory.AUDIT, EventType.CREATE_AUCTION, 
                "User=" + user.username() + " Auction=" + auctionId);
            return auctionId;
        });
    }

    public void cancelAuction(int auctionId, SessionContext user) throws Exception {
        lockManager.lock(auctionId);
        try {
            txManager.executeWithoutResult(() -> {
                AuctionItem item = auctionRepo.findAuctionById(auctionId);
                if (item == null) throw new AuctionException("Auction not found");
                
                if (!item.getSellerUsername().equals(user.username()) && !Constants.ADMIN.equals(user.role())) {
                    throw new AuctionException("Only the seller or an admin can cancel this auction");
                }
                
                if (bidRepo.countBidsByAuctionId(auctionId) > 0) {
                    throw new AuctionException("Cannot cancel auction with active bids");
                }
                
                auctionRepo.updateAuctionStatus(auctionId, Constants.STATUS_CANCELLED);
                AsyncLogger.log(LogCategory.AUDIT, EventType.CANCEL_AUCTION, 
                    "User=" + user.username() + " Auction=" + auctionId);
            });
        } finally {
            lockManager.unlock(auctionId);
        }
    }

    public int relistAuction(int auctionId, String newEndTimeIso, SessionContext user) throws Exception {
        lockManager.lock(auctionId);
        try {
            return txManager.execute(() -> {
                AuctionItem parent = auctionRepo.findAuctionById(auctionId);
                if (parent == null) throw new AuctionException("Parent auction not found");

                if (!parent.getSellerUsername().equals(user.username()) && !Constants.ADMIN.equals(user.role())) {
                    throw new AuctionException("Only the seller or an admin can relist this auction");
                }

                if (!Constants.STATUS_EXPIRED.equals(parent.getStatus())) {
                    throw new AuctionException("Only expired auctions can be relisted");
                }

                if (bidRepo.countBidsByAuctionId(auctionId) > 0) {
                    throw new AuctionException("Cannot relist an auction that has bids");
                }

                Instant now = Instant.now();
                Instant newEndTime = Instant.parse(newEndTimeIso);
                if (!newEndTime.isAfter(now)) {
                    throw new AuctionException("New end time must be in the future");
                }

                AuctionItem child = new AuctionItem();
                child.setTitle(parent.getTitle());
                child.setDescription(parent.getDescription());
                child.setCategory(parent.getCategory());
                child.setStartingPriceCents(parent.getStartingPriceCents());
                child.setCurrentBidCents(parent.getStartingPriceCents());
                child.setSellerUsername(parent.getSellerUsername());
                child.setStartTime(now.toString());
                child.setEndTime(newEndTimeIso);
                child.setCapEndTime(newEndTime.plus(Duration.ofMinutes(Constants.SNIPE_CAP_DEFAULT_MINUTES)).toString());
                child.setStatus(Constants.STATUS_ACTIVE);
                child.setImg1(parent.getImg1());
                child.setImg2(parent.getImg2());
                child.setImg3(parent.getImg3());
                child.setRelistedFrom(auctionId);

                int newId = auctionRepo.insertAuction(child);
                AsyncLogger.log(LogCategory.AUDIT, EventType.RELIST_AUCTION, 
                    "User=" + user.username() + " OldAuction=" + auctionId + " NewAuction=" + newId);
                return newId;
            });
        } finally {
            lockManager.unlock(auctionId);
        }
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

    private void validateFreshness(AuctionItem item, long expectedCents) throws StaleDataException {
        if (expectedCents != item.getCurrentBidCents()) {
            throw new StaleDataException("Price has changed. Please refresh and try again.");
        }
    }

    private void validateMinimumBid(AuctionItem item, long amountCents) throws InsufficientBidException {
        if (amountCents <= 0) {
            throw new InsufficientBidException("Bid amount must be positive");
        }
        if (item.getHighestBidderUsername() == null) {
            if (amountCents < item.getStartingPriceCents()) {
                throw new InsufficientBidException("Bid must be at least the starting price of " + Constants.formatCents(item.getStartingPriceCents()));
            }
        } else {
            long minBidCents = item.getCurrentBidCents() + (item.getCurrentBidCents() + 19) / 20;
            if (amountCents < minBidCents) {
                throw new InsufficientBidException("Bid must be at least " + Constants.formatCents(minBidCents) + " (5% increment)");
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

    private String applySnipeProtection(AuctionItem item, Instant now) {
        Instant endTime = Instant.parse(item.getEndTime());
        Instant capEndTime;
        if (item.getCapEndTime() != null) {
            capEndTime = Instant.parse(item.getCapEndTime());
        } else {
            capEndTime = endTime.plus(Duration.ofMinutes(Constants.SNIPE_CAP_DEFAULT_MINUTES));
        }

        if (Duration.between(now, endTime).getSeconds() < 30) {
            Instant extendedTime = now.plusSeconds(30);
            if (extendedTime.isAfter(capEndTime)) {
                extendedTime = capEndTime;
            }
            if (extendedTime.isAfter(endTime)) {
                return extendedTime.toString();
            }
        }
        return item.getEndTime();
    }
}
