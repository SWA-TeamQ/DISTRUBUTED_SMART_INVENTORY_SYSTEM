package com.auction.client.core;

import com.auction.client.network.RmiClientProvider;
import com.auction.client.network.UdpDiscoveryClient;
import com.auction.client.ui.ViewLoader;

public class ClientContext {
    private static final ClientContext INSTANCE = new ClientContext();

    private RmiClientProvider rmiProvider;
    private UdpDiscoveryClient udpClient;
    private ViewLoader viewLoader;
    private String sessionToken;
    private String userRole;
    private String username;
    private String previousViewName;
    private int currentAuctionId = -1;

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
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPreviousViewName() { return previousViewName; }
    public void setPreviousViewName(String previousViewName) { this.previousViewName = previousViewName; }

    public int getCurrentAuctionId() { return currentAuctionId; }
    public void setCurrentAuctionId(int currentAuctionId) { this.currentAuctionId = currentAuctionId; }

    public void clearSession() {
        this.sessionToken = null;
        this.userRole = null;
        this.username = null;
        this.previousViewName = null;
    }

    public void handleConnectionLost() {
        getRmiProvider().reset();
        try {
            getViewLoader().loadView("connect.fxml");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
