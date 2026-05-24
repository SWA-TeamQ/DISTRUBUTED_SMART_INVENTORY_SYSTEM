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
        String host = ipField.getText();
        if (host == null || host.trim().isEmpty()) {
            host = "localhost";
        }
        int port = 1099;
        try {
            String portStr = portField.getText();
            if (portStr != null && !portStr.trim().isEmpty()) {
                port = Integer.parseInt(portStr);
            }
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid port number.");
            return;
        }

        try {
            com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
            context.getRmiProvider().connect(host, port);
            statusLabel.setText("Connected successfully!");
            context.getUdpClient().stopListening();
            
            // Navigate to login
            context.getViewLoader().loadView("login.fxml");
        } catch (Exception e) {
            statusLabel.setText("Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
