package com.auction.server.service;

import com.auction.server.repository.AuctionRepository;
import com.auction.shared.Constants;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background thread that runs every 1 second.
 * Scans for ACTIVE auctions past their end_time and transitions them:
 *   - SOLD (if bids exist)
 *   - EXPIRED (if no bids)
 * Also handles crash recovery on startup.
 */
public class AuctionReaper {

    private final AuctionRepository auctionRepo;
    private ScheduledExecutorService scheduler;

    public AuctionReaper(AuctionRepository auctionRepo) {
        this.auctionRepo = auctionRepo;
    }

    /** Start the reaper. Call once at server startup. */
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AuctionReaper");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::reap, 0,
                Constants.REAPER_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /** Stop the reaper. Call on server shutdown. */
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    /**
     * Single reap cycle. Finds overdue ACTIVE auctions, transitions them.
     * Package-private for testing.
     */
    void reap() {
        // TODO: query for ACTIVE auctions where end_time < now
        // TODO: for each, check if bids exist -> SOLD or EXPIRED
        // TODO: log transitions to audit log
    }

    /** Crash recovery: expire all overdue ACTIVE auctions. Call once at startup. */
    public void recoverFromCrash() {
        reap(); // same logic
    }
}
