package com.auction.server.core;

import com.auction.shared.Constants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Broadcasts server presence via UDP every 2 seconds.
 * Packet format: "RTDAS|<rmiPort>|<serverName>"
 * Clients listen on UDP_BROADCAST_PORT to auto-discover.
 */
public class UdpBroadcaster {

    private final int rmiPort;
    private final String serverName;
    private ScheduledExecutorService scheduler;

    public UdpBroadcaster(int rmiPort, String serverName) {
        this.rmiPort = rmiPort;
        this.serverName = serverName;
    }

    /** Start broadcasting. Call once at server startup. */
    public void start() {
        // TODO: open DatagramSocket, schedule broadcast every UDP_BROADCAST_INTERVAL_MS
    }

    /** Stop broadcasting. Call on server shutdown. */
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }
}
