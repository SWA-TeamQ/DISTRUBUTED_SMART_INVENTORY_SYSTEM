package com.auction.client.network;

import com.auction.shared.Constants;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
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

    /**
     * Start listening for server broadcasts.
     * @return true if discovery listening started, false if the port is already in use.
     */
    public boolean startListening() {
        if (running) return true;
        running = true;
        final DatagramSocket socket;
        try {
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(Constants.UDP_BROADCAST_PORT));
            socket.setSoTimeout(1000); // 1 second timeout to allow interrupt checking
        } catch (Exception e) {
            running = false;
            System.err.println("[RTDAS] UDP discovery unavailable: " + e.getMessage());
            return false;
        }

        listenerThread = new Thread(() -> {
            try (DatagramSocket boundSocket = socket) {
                byte[] buffer = new byte[1024];
                while (running) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        boundSocket.receive(packet);
                        String data = new String(packet.getData(), 0, packet.getLength()).trim();
                        // Format: RTDAS|<ServerName>|<RmiPort>
                        if (data.startsWith(Constants.UDP_PREFIX + "|")) {
                            String[] parts = data.split("\\|");
                            if (parts.length == 3) {
                                String serverName = parts[1];
                                int rmiPort = Integer.parseInt(parts[2]);
                                String host = packet.getAddress().getHostAddress();
                                ServerInfo info = new ServerInfo(serverName, host, rmiPort);
                                if (!discoveredServers.contains(info)) {
                                    discoveredServers.add(info);
                                }
                            }
                        }
                    } catch (java.net.SocketTimeoutException e) {
                        // Expected timeout, loop continues and checks running flag
                    }
                }
            } catch (Exception e) {
                if (running) {
                    System.err.println("[RTDAS] UDP discovery unavailable: " + e.getMessage());
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
        return true;
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
