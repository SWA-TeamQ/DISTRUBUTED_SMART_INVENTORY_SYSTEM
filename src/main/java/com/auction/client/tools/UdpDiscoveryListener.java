package com.auction.client.tools;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Simple UDP listener for discovery packets on port 9999.
 * Usage: run and it will print any discovery packets received for 8 seconds.
 */
public class UdpDiscoveryListener {

  public static void main(String[] args) throws Exception {
    int port = 9999;
    System.out.println(
      "Listening for UDP discovery on port " + port + " for 8 seconds..."
    );
    try (DatagramSocket socket = new DatagramSocket(port)) {
      socket.setSoTimeout(8000);
      byte[] buf = new byte[1024];
      DatagramPacket p = new DatagramPacket(buf, buf.length);
      long end = System.currentTimeMillis() + 8000;
      while (System.currentTimeMillis() < end) {
        try {
          socket.receive(p);
          String data = new String(p.getData(), 0, p.getLength()).trim();
          System.out.println(
            "Received from " +
              p.getAddress().getHostAddress() +
              ":" +
              p.getPort() +
              " => " +
              data
          );
        } catch (java.net.SocketTimeoutException ste) {
          // ignore and loop until timeout
        }
      }
    }
    System.out.println("Listener finished.");
  }
}
