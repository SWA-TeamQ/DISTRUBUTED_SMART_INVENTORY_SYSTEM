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

    public void clearSession() {
        this.sessionToken = null;
        this.userRole = null;
        this.username = null;
    }
}
