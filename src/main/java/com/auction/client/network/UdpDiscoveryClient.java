package com.auction.client.network;

import com.auction.shared.Constants;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Listens for UDP broadcast packets from RTDAS servers on the local network.
 * Discovered servers are stored as ServerInfo records.
 * Runs on a background thread; call stop() to clean up.
 */
public class UdpDiscoveryClient {
    
    /** Represents a discovered server. */
    public record ServerInfo(String name, String host, int rmiPort) {
        @Override
        public String toString() {
            return name + " (" + host + ":" + rmiPort + ")";
        }
    }

    private final List<ServerInfo> discoveredServers = new CopyOnWriteArrayList<>();
    private volatile boolean running;
    private Thread listenerThread;

    /** Start listening for server broadcasts. */
    public void startListening() {
        // TODO: open DatagramSocket on UDP_BROADCAST_PORT
        // TODO: parse packets matching UDP_PREFIX format
        // TODO: add to discoveredServers (avoid duplicates)
    }

    /** Stop listening. */
    public void stopListening() {
        running = false;
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }

    /** Get list of currently discovered servers. */
    public List<ServerInfo> getDiscoveredServers() {
        return List.copyOf(discoveredServers);
    }
}
