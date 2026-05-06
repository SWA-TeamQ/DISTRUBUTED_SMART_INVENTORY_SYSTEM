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
}
