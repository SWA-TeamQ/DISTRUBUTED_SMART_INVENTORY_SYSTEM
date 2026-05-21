package com.auction.client.service;

import com.auction.shared.interfaces.IAuctionService;
import com.auction.shared.models.AuctionItem;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Polls the server every 2 seconds for auction updates.
 * Results dispatched to a callback (should use Platform.runLater in controller).
 * Auto-stops when shutdown() is called.
 */
public class PollingService {

    private final IAuctionService service;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> pollingTask;
    private final AtomicBoolean paused = new AtomicBoolean(false);

    public PollingService(IAuctionService service) {
        this.service = service;
    }

    /**
     * Start polling a specific auction for updates.
     * @param auctionId auction to watch
     * @param onUpdate callback with updated AuctionItem (called on background thread)
     */
    public void startPolling(int auctionId, Consumer<AuctionItem> onUpdate) {
        shutdown();
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "auction-polling");
            thread.setDaemon(true);
            return thread;
        });
        paused.set(false);
        pollingTask = scheduler.scheduleAtFixedRate(() -> {
            if (paused.get()) {
                return;
            }
            try {
                AuctionItem item = service.getAuctionById(auctionId);
                if (item != null) {
                    onUpdate.accept(item);
                }
            } catch (Exception ex) {
                paused.set(true);
                System.err.println("Polling stopped after error: " + ex.getMessage());
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    public void pause() {
        paused.set(true);
    }

    public void resume() {
        paused.set(false);
    }

    /** Stop all polling. Call when leaving the detail view. */
    public void shutdown() {
        if (pollingTask != null && !pollingTask.isCancelled()) {
            pollingTask.cancel(true);
        }
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }
}
