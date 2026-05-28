package com.auction.server.service;

import com.auction.server.repository.AuctionRepository;
import com.auction.server.core.LifecycleManager;
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

    private final LifecycleManager lifecycleManager;
    private ScheduledExecutorService scheduler;

    public AuctionReaper(LifecycleManager lifecycleManager) {
        this.lifecycleManager = lifecycleManager;
    }

    /** Start the reaper. Call once at server startup. */
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AuctionReaper");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            try {
                lifecycleManager.activateScheduled();
            } catch (Exception e) {
                System.err.println("Error activating scheduled auctions: " + e.getMessage());
            }
            lifecycleManager.sweepOverdue();
        }, 0, Constants.REAPER_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /** Stop the reaper. Call on server shutdown. */
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    /** Crash recovery: expire all overdue ACTIVE auctions. Call once at startup. */
    public void recoverFromCrash() {
        lifecycleManager.sweepOverdue();
    }
}

