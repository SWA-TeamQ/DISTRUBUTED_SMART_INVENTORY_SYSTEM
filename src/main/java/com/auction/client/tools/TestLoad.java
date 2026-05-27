package com.auction.client.tools;

import java.net.URL;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class TestLoad {

  public static void main(String[] args) {
    Platform.startup(() -> {
      try {
        URL resource = TestLoad.class.getResource("/fxml/user_dashboard.fxml");
        System.out.println("Resource: " + resource);
        Parent root = FXMLLoader.load(resource);
        System.out.println("Loaded successfully!");
      } catch (Exception e) {
        e.printStackTrace();
        if (e.getCause() != null) {
          System.out.println("CAUSE:");
          e.getCause().printStackTrace();
          if (e.getCause().getCause() != null) {
            System.out.println("ROOT CAUSE:");
            e.getCause().getCause().printStackTrace();
          }
        }
      }
      Platform.exit();
    });
  }
}
