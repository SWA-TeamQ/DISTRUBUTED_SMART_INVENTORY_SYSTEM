package com.auction.client.service;

import com.auction.shared.interfaces.IAuctionService;
import com.auction.shared.models.AuctionItem;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Polls the server every 2 seconds for auction updates.
 * Results dispatched to a callback (should use Platform.runLater in controller).
 * Auto-stops when shutdown() is called.
 */
public class PollingService {

    private ScheduledExecutorService scheduler;
    private int failureCount = 0;

    public PollingService() {
        // generic service
    }

    /**
     * Start polling a generic task.
     * @param task the RMI call to execute
     * @param intervalSeconds polling interval
     */
    public void startPolling(Runnable task, int intervalSeconds) {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                task.run();
                failureCount = 0; // reset on success
            } catch (Exception e) {
                failureCount++;
                System.err.println("Polling failed (" + failureCount + "): " + e.getMessage());
                if (failureCount >= 3) {
                    shutdown();
                    javafx.application.Platform.runLater(() -> {
                        com.auction.client.core.ClientContext.getInstance().handleConnectionLost();
                    });
                }
            }
        }, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    /** Stop all polling. Call when leaving the view. */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }
}
