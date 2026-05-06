package com.auction.server.core;

import com.auction.shared.Constants;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Server entry point. Bootstraps RMI registry, binds AuctionService,
 * starts AuctionReaper, starts UDP broadcaster.
 */
public class ServerLauncher {

    public static void main(String[] args) {
        // TODO: init DatabaseManager (creates schema + seeds admin)
        // TODO: create AuctionServiceImpl, export as UnicastRemoteObject
        // TODO: create/start RMI registry on Constants.DEFAULT_RMI_PORT
        // TODO: bind service to registry under Constants.RMI_SERVICE_NAME
        // TODO: start AuctionReaper
        // TODO: start UdpBroadcaster
        // TODO: recover overdue ACTIVE auctions from crash
        System.out.println("[RTDAS] Server skeleton loaded. Implementation pending.");
    }
}
