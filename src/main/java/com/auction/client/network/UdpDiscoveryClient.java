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
        if (running) return;
        running = true;
        listenerThread = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(Constants.UDP_BROADCAST_PORT)) {
                socket.setSoTimeout(1000); // 1 second timeout to allow interrupt checking
                byte[] buffer = new byte[1024];
                while (running) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        String data = new String(packet.getData(), 0, packet.getLength()).trim();
                        // Format: RTDAS|v1|<rmiPort>|<serverName>|<serverId>|<rmiHost>
                        if (data.startsWith(Constants.UDP_PREFIX + "|v1|")) {
                            String[] parts = data.split("\\|");
                            if (parts.length >= 6) {
                                int rmiPort = Integer.parseInt(parts[2]);
                                String serverName = parts[3];
                                String rmiHost = parts[5];
                                String host = (rmiHost != null && !rmiHost.trim().isEmpty()) ? rmiHost : packet.getAddress().getHostAddress();
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
                    e.printStackTrace();
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
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
