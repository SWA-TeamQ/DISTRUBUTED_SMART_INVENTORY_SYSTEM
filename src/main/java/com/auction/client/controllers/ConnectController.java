package com.auction.client.controllers;

import com.auction.client.core.ClientContext;
import com.auction.client.network.UdpDiscoveryClient.ServerInfo;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.util.List;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ConnectController {

  @FXML
  private ListView<ServerInfo> serverListView;

  @FXML
  private TextField ipField, portField;

  @FXML
  private Label statusLabel;

  @FXML
  public void initialize() {
    ClientContext ctx = ClientContext.getInstance();
    ctx.getUdpClient().startListening();

    startDiscoveryTask(ctx);

    serverListView
      .getSelectionModel()
      .selectedItemProperty()
      .addListener((obs, old, newVal) -> {
        if (newVal != null) {
          ipField.setText(newVal.host());
          portField.setText(String.valueOf(newVal.rmiPort()));
        }
      });
  }

  private void startDiscoveryTask(ClientContext ctx) {
    Thread task = new Thread(() -> {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          Thread.sleep(1000);
          List<ServerInfo> servers = ctx.getUdpClient().getDiscoveredServers();
          Platform.runLater(() -> {
            if (servers == null || servers.isEmpty()) {
              serverListView.getItems().clear();
              statusLabel.setText(
                "No server discovered. Please enter details manually."
              );
            } else {
              serverListView.getItems().setAll(servers);
              statusLabel.setText(
                "Discovered " + servers.size() + " server(s)."
              );
            }
          });
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });
    task.setDaemon(true);
    task.start();
  }

  @FXML
  private void handleConnect() {
    String host = ipField.getText().trim().isEmpty()
      ? "localhost"
      : ipField.getText().trim();
    int port;
    try {
      port = Integer.parseInt(portField.getText().trim());
    } catch (Exception e) {
      statusLabel.setText("Invalid port.");
      return;
    }

    statusLabel.setText("Connecting...");
    System.setProperty("sun.rmi.transport.tcp.connectTimeout", "3000");

    new Thread(() -> {
      try {
        ClientContext ctx = ClientContext.getInstance();
        ctx.getRmiProvider().connect(host, port);
        Platform.runLater(() -> {
          statusLabel.setText("Connected!");
          ctx.getUdpClient().stopListening();
          try {
            ctx.getViewLoader().loadView("login.fxml");
          } catch (Exception e) {
            statusLabel.setText("View load error.");
          }
        });
      } catch (Exception e) {
        Platform.runLater(() ->
          statusLabel.setText("Failed: " + formatError(e))
        );
      }
    })
      .start();
  }

  private String formatError(Exception e) {
    Throwable t = e;
    while (t.getCause() != null) t = t.getCause();
    if (t instanceof ConnectException) return "Server unreachable.";
    if (t instanceof NotBoundException) return "Service not found.";
    return t.getMessage();
  }
}
