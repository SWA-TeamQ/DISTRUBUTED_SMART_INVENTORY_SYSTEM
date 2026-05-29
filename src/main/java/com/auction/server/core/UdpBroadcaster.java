package com.auction.server.core;

import com.auction.shared.Constants;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
    if (scheduler != null) return;

    scheduler = Executors.newSingleThreadScheduledExecutor();
    String message = Constants.UDP_PREFIX + "|" + serverName + "|" + rmiPort;
    byte[] data = message.getBytes();

    scheduler.scheduleAtFixedRate(
      () -> {
        try (DatagramSocket socket = new DatagramSocket()) {
          socket.setBroadcast(true);
          InetAddress address = InetAddress.getByName("255.255.255.255");
          DatagramPacket packet = new DatagramPacket(
            data,
            data.length,
            address,
            Constants.UDP_BROADCAST_PORT
          );
          socket.send(packet);
        } catch (IOException e) {
          System.err.println("[RTDAS] Broadcaster error: " + e.getMessage());
        }
      },
      0,
      Constants.UDP_BROADCAST_INTERVAL_MS,
      TimeUnit.MILLISECONDS
    );
  }

  /** Stop broadcasting. Call on server shutdown. */
  public void stop() {
    if (scheduler != null) {
      scheduler.shutdownNow();
      scheduler = null;
    }
  }
}
