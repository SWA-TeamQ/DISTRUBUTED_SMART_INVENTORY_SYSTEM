package com.auction.server.core;

import java.net.InetAddress;

/**
 * Server entry point.
 */
public class ServerLauncher {

  public static void main(String[] args) {
    try {
      int rmiPort = resolveRmiPort(args);

      // Ensure RMI advertises a reachable hostname. If the JVM flag is
      // not provided, attempt to auto-detect and set it so clients can
      // connect when the server isn't bound to the loopback address.
      String existing = System.getProperty("java.rmi.server.hostname");
      if (existing == null || existing.isBlank()) {
        try {
          String hostAddr = InetAddress.getLocalHost().getHostAddress();
          System.setProperty("java.rmi.server.hostname", hostAddr);
          System.out.println(
            "[RTDAS] Set java.rmi.server.hostname to " + hostAddr
          );
        } catch (Exception ex) {
          System.err.println(
            "[RTDAS] Failed to auto-detect hostname: " + ex.getMessage()
          );
        }
      } else {
        System.out.println("[RTDAS] java.rmi.server.hostname=" + existing);
      }

      System.out.println("[RTDAS] Initializing server...");
      System.out.println("[RTDAS] Using RMI port: " + rmiPort);

      ServerBootstrap bootstrap = new ServerBootstrap(rmiPort);
      bootstrap.start();
    } catch (Exception e) {
      System.err.println("[RTDAS] Server failed to start: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static int resolveRmiPort(String[] args) {
    int defaultPort = com.auction.shared.Constants.DEFAULT_RMI_PORT;
    if (
      args == null || args.length == 0 || args[0] == null || args[0].isBlank()
    ) {
      return defaultPort;
    }

    try {
      int parsed = Integer.parseInt(args[0].trim());
      if (parsed < 1 || parsed > 65535) {
        System.err.println(
          "[RTDAS] Invalid RMI port '" +
            args[0] +
            "'. Falling back to default: " +
            defaultPort
        );
        return defaultPort;
      }
      return parsed;
    } catch (NumberFormatException ex) {
      System.err.println(
        "[RTDAS] Invalid RMI port '" +
          args[0] +
          "'. Falling back to default: " +
          defaultPort
      );
      return defaultPort;
    }
  }
}
