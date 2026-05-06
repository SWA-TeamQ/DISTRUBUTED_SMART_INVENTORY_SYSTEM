package com.auction.client.network;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RmiClientProvider {
    public Registry connect(String host, int port) throws RemoteException {
        return LocateRegistry.getRegistry(host, port);
    }
}
