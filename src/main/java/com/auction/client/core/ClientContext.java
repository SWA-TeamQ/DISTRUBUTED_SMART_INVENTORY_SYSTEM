package com.auction.client.core;

import com.auction.client.network.RmiClientProvider;
import com.auction.client.network.UdpDiscoveryClient;
import com.auction.client.ui.ViewLoader;
import com.auction.shared.models.AuctionItem;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Modality;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ClientContext {
    private static final ClientContext INSTANCE = new ClientContext();
    private static final long AUTO_LAUNCH_POLL_SECONDS = 2L;

    private RmiClientProvider rmiProvider;
    private UdpDiscoveryClient udpClient;
    private ViewLoader viewLoader;
    private String sessionToken;
    private String userRole;
    private String username;
    private String previousViewName;
    private int currentAuctionId = -1;
    private final ScheduledExecutorService automaticLaunchWatcher = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "auto-launch-watcher");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<Integer, String> lastSeenAuctionStatus = new ConcurrentHashMap<>();
    private final Set<Integer> notifiedAuctionIds = ConcurrentHashMap.newKeySet();
    private volatile boolean automaticLaunchWatcherRunning = false;
    private volatile ScheduledFuture<?> automaticLaunchWatcherTask;

    private ClientContext() {
        rmiProvider = new RmiClientProvider();
        udpClient = new UdpDiscoveryClient();
    }

    public static ClientContext getInstance() {
        return INSTANCE;
    }

    public RmiClientProvider getRmiProvider() { return rmiProvider; }
    public UdpDiscoveryClient getUdpClient() { return udpClient; }
    public ViewLoader getViewLoader() { return viewLoader; }
    public void setViewLoader(ViewLoader viewLoader) { this.viewLoader = viewLoader; }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
        if (sessionToken == null || sessionToken.isBlank()) {
            stopAutomaticLaunchWatcher();
        } else {
            startAutomaticLaunchWatcher();
        }
    }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPreviousViewName() { return previousViewName; }
    public void setPreviousViewName(String previousViewName) { this.previousViewName = previousViewName; }

    public int getCurrentAuctionId() { return currentAuctionId; }
    public void setCurrentAuctionId(int currentAuctionId) { this.currentAuctionId = currentAuctionId; }

    public void clearSession() {
        stopAutomaticLaunchWatcher();
        this.sessionToken = null;
        this.userRole = null;
        this.username = null;
        this.previousViewName = null;
        this.currentAuctionId = -1;
        this.lastSeenAuctionStatus.clear();
        this.notifiedAuctionIds.clear();
    }

    public void handleConnectionLost() {
        getRmiProvider().reset();
        try {
            getViewLoader().loadView("connect.fxml");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void startAutomaticLaunchWatcher() {
        if (automaticLaunchWatcherRunning) {
            return;
        }
        automaticLaunchWatcherRunning = true;
        automaticLaunchWatcherTask = automaticLaunchWatcher.scheduleAtFixedRate(() -> {
            try {
                String token = sessionToken;
                if (token == null || token.isBlank()) {
                    return;
                }

                java.util.List<AuctionItem> auctions = getRmiProvider().getService().getAllAuctions();
                if (auctions == null || auctions.isEmpty()) {
                    return;
                }

                for (AuctionItem item : auctions) {
                    if (item == null || item.getId() <= 0) {
                        continue;
                    }

                    String currentStatus = item.getStatus();
                    String previousStatus = lastSeenAuctionStatus.put(item.getId(), currentStatus);
                    if (previousStatus == null) {
                        continue;
                    }

                    boolean becameActive = !com.auction.shared.Constants.STATUS_ACTIVE.equalsIgnoreCase(previousStatus)
                        && com.auction.shared.Constants.STATUS_ACTIVE.equalsIgnoreCase(currentStatus);
                    boolean automaticStart = com.auction.shared.Constants.START_MODE_AUTO.equalsIgnoreCase(item.getStartMode());

                    if (becameActive && automaticStart && notifiedAuctionIds.add(item.getId())) {
                        Platform.runLater(() -> showAutomaticLaunchNotice(item));
                    }
                }
            } catch (Exception ignored) {
                // Best-effort background watcher; temporary failures should not interrupt the session.
            }
        }, AUTO_LAUNCH_POLL_SECONDS, AUTO_LAUNCH_POLL_SECONDS, TimeUnit.SECONDS);
    }

    private synchronized void stopAutomaticLaunchWatcher() {
        automaticLaunchWatcherRunning = false;
        ScheduledFuture<?> task = automaticLaunchWatcherTask;
        if (task != null) {
            task.cancel(false);
            automaticLaunchWatcherTask = null;
        }
    }

    private void showAutomaticLaunchNotice(AuctionItem item) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initModality(Modality.NONE);
        alert.setTitle("Auction Launched");
        alert.setHeaderText("Auction launched automatically");
        alert.setContentText(
            item == null
                ? "An auction is now live and accepting bids."
                : "Auction #" + item.getId() + " is now live and accepting bids."
        );
        alert.show();
    }
}
