package com.auction.client.service;

import com.auction.shared.interfaces.IAuctionService;
import com.auction.shared.models.AuctionItem;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Polls the server every 1 second for auction updates.
 * Results dispatched to a callback (should use Platform.runLater in controller).
 * Auto-stops when shutdown() is called.
 */
public class PollingService {

    private final IAuctionService service;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTask;
    private volatile boolean paused = false;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final int baseIntervalSeconds;
    private final int maxBackoffSeconds;
    private final int maxFailuresBeforeNotify;
    private Consumer<Throwable> onFailure;
    private Consumer<AuctionItem> onUpdateCallback;
    private volatile boolean running = false;

    public PollingService(IAuctionService service) {
        this(service, 1, 3, 32);
    }

    /**
     * Constructor with tunable timing for tests.
     */
    public PollingService(IAuctionService service, int baseIntervalSeconds, int maxFailuresBeforeNotify, int maxBackoffSeconds) {
        this.service = service;
        this.baseIntervalSeconds = Math.max(1, baseIntervalSeconds);
        this.maxFailuresBeforeNotify = Math.max(1, maxFailuresBeforeNotify);
        this.maxBackoffSeconds = Math.max(baseIntervalSeconds, maxBackoffSeconds);
    }

    /**
     * Start polling a specific auction for updates.
     * @param auctionId auction to watch
     * @param onUpdate callback with updated AuctionItem (called on background thread)
     */
    public void startPolling(int auctionId, Consumer<AuctionItem> onUpdate) {
        startPolling(auctionId, onUpdate, null);
    }

    /**
     * Start polling with optional failure callback. onFailure is invoked after
     * {@code maxFailuresBeforeNotify} consecutive errors.
     */
    public void startPolling(int auctionId, Consumer<AuctionItem> onUpdate, Consumer<Throwable> onFailure) {
        if (running) return;
        this.onFailure = onFailure;
        this.onUpdateCallback = onUpdate;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        running = true;
        scheduleNext(0, auctionId);
    }

    private void scheduleNext(long delaySeconds, int auctionId) {
        scheduledTask = scheduler.schedule(() -> {
            if (!running) return;
            if (paused) {
                // If paused, reschedule without touching failure counters
                scheduleNext(baseIntervalSeconds, auctionId);
                return;
            }
            try {
                AuctionItem item = service.getAuctionById(auctionId);
                failureCount.set(0);
                if (onUpdateCallback != null) onUpdateCallback.accept(item);
                scheduleNext(baseIntervalSeconds, auctionId);
            } catch (Exception e) {
                int fails = failureCount.incrementAndGet();
                if (fails >= maxFailuresBeforeNotify && onFailure != null) {
                    onFailure.accept(e);
                }
                // exponential backoff
                long nextDelay = Math.min(maxBackoffSeconds, baseIntervalSeconds * (1L << Math.min(30, fails)));
                scheduleNext(nextDelay, auctionId);
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    /** Stop all polling. Call when leaving the detail view. */
    public void shutdown() {
        running = false;
        paused = false;
        if (scheduledTask != null) scheduledTask.cancel(true);
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    /** Pause polling (keeps scheduler alive). */
    public void pause() { this.paused = true; }

    /** Resume polling after a pause. */
    public void resume() { this.paused = false; }

    public boolean isPaused() { return this.paused; }

    public boolean isRunning() { return this.running; }

    /** Set a failure callback after startPolling if needed. */
    public void setOnFailure(Consumer<Throwable> onFailure) { this.onFailure = onFailure; }
}
