package com.auction.client.network;

import com.auction.shared.Constants;
import com.auction.shared.interfaces.IAuctionService;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Manages the RMI connection to the server.
 * Provides the IAuctionService stub for all remote calls.
 */
public class RmiClientProvider {

    private IAuctionService service;

    /**
     * Connect to the RMI registry and look up the auction service.
     * @param host server IP address
     * @param port RMI registry port
     * @return the remote service stub
     */
    public IAuctionService connect(String host, int port) throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(host, port);
        service = (IAuctionService) registry.lookup(Constants.RMI_SERVICE_NAME);
        
        // Health check
        service.serverTime();
        
        // Persist last server
        try {
            java.nio.file.Path dir = java.nio.file.Paths.get(System.getProperty("user.home"), ".rtdas");
            if (!java.nio.file.Files.exists(dir)) {
                java.nio.file.Files.createDirectories(dir);
            }
            java.nio.file.Files.writeString(dir.resolve("last_server"), host + ":" + port);
        } catch (java.io.IOException e) {
            System.err.println("Failed to persist last_server: " + e.getMessage());
        }
        
        return service;
    }

    /** Get the cached service stub. Null if not connected. */
    public IAuctionService getService() {
        return service;
    }

    /** Check if currently connected. */
    public boolean isConnected() {
        return service != null;
    }

    /** Clear the current service stub. */
    public void reset() {
        this.service = null;
    }
}
