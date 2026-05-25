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

    private final IAuctionService service;
    private ScheduledExecutorService scheduler;

    public PollingService(IAuctionService service) {
        this.service = service;
    }

    /**
     * Start polling a specific auction for updates.
     * @param auctionId auction to watch
     * @param onUpdate callback with updated AuctionItem (called on background thread)
     */
    public void startPolling(int auctionId, Consumer<AuctionItem> onUpdate) {
        // TODO: schedule getAuctionById every 2s, call onUpdate with result
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try{
                AuctionItem item = service.getAuctionById(auctionId);
                onUpdate.accept(item);
            } catch(Exception e){
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    /** Stop all polling. Call when leaving the detail view. */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }
}
