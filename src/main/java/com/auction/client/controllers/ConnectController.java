package com.auction.client.controllers;

import javafx.fxml.FXML;

public class ConnectController {
    
    @FXML private javafx.scene.control.ListView<com.auction.client.network.UdpDiscoveryClient.ServerInfo> serverListView;
    @FXML private javafx.scene.control.TextField ipField;
    @FXML private javafx.scene.control.TextField portField;
    @FXML private javafx.scene.control.Label statusLabel;

    @FXML
    public void initialize() {
        com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
        context.getUdpClient().startListening();

        javafx.application.Platform.runLater(() -> statusLabel.setText("Waiting for discovered servers. You can edit IP and port manually."));

        // Start a background thread to update the list view
        Thread updateThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    java.util.List<com.auction.client.network.UdpDiscoveryClient.ServerInfo> servers = context.getUdpClient().getDiscoveredServers();
                    javafx.application.Platform.runLater(() -> {
                        if (servers == null || servers.isEmpty()) {
                            serverListView.getItems().clear();
                            if (ipField.getText() == null || ipField.getText().isBlank()) {
                                ipField.setText("localhost");
                            }
                            if (portField.getText() == null || portField.getText().isBlank()) {
                                portField.setText("1099");
                            }
                            statusLabel.setText("No discovered server yet. Localhost is prefilled and can be edited.");
                        } else {
                            serverListView.getItems().setAll(servers);
                            statusLabel.setText("Discovered " + servers.size() + " server(s).");
                        }
                    });
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();
        
        serverListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                ipField.setText(newVal.host());
                portField.setText(String.valueOf(newVal.rmiPort()));
            }
        });
    }

    @FXML
    private void handleConnect() {
        String hostInput = ipField.getText();
        String portInput = portField.getText();

        String host = (hostInput == null || hostInput.trim().isEmpty()) ? "localhost" : hostInput.trim();
        int port = 1099;
        if (portInput != null && !portInput.trim().isEmpty()) {
            try {
                port = Integer.parseInt(portInput.trim());
            } catch (NumberFormatException e) {
                statusLabel.setText("Invalid port number.");
                return;
            }
        }

        statusLabel.setText("Connecting to " + host + ":" + port + "...");
        
        final String finalHost = host;
        final int finalPort = port;

        // Set short timeout for RMI connection attempts
        System.setProperty("sun.rmi.transport.tcp.connectTimeout", "3000");

        // Use a standard Thread to avoid any issues with common pool size or configuration
        new Thread(() -> {
            try {
                com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
                // Perform the RMI connection and health check
                context.getRmiProvider().connect(finalHost, finalPort);
                
                javafx.application.Platform.runLater(() -> {
                    try {
                        statusLabel.setText("Connected successfully!");
                        context.getUdpClient().stopListening();
                        // Navigate to login
                        context.getViewLoader().loadView("login.fxml");
                    } catch (Exception e) {
                        statusLabel.setText("Navigation failed: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    Throwable root = e;
                    while (root.getCause() != null && root.getCause() != root) {
                        root = root.getCause();
                    }
                    
                    String errorMsg;
                    if (root instanceof java.net.ConnectException || root instanceof java.rmi.ConnectException) {
                        errorMsg = "Server unreachable at " + finalHost + ":" + finalPort;
                    } else if (e instanceof java.rmi.NotBoundException) {
                        errorMsg = "RTDAS service not found on this server.";
                    } else {
                        errorMsg = (root.getMessage() != null) ? root.getMessage() : root.toString();
                    }
                    statusLabel.setText("Connection failed: " + errorMsg);
                });
            }
        }).start();
    }
}
