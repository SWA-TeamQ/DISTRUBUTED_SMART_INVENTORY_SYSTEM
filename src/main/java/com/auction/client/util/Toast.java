package com.auction.client.util;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

public final class Toast {

  public enum Type {
    INFO,
    SUCCESS,
    ERROR,
  }

  private Toast() {}

  public static void show(Node ownerNode, String message) {
    show(ownerNode, message, Type.INFO);
  }

  public static void show(Node ownerNode, String message, Type type) {
    if (message == null || message.isBlank()) {
      return;
    }

    Platform.runLater(() -> {
      Scene scene = ownerNode == null ? null : ownerNode.getScene();
      Window window = scene == null ? null : scene.getWindow();
      if (window == null) {
        return;
      }

      Popup popup = new Popup();
      Label label = new Label(message);
      label.setWrapText(true);
      label.setMaxWidth(320);
      label.getStyleClass().addAll("toast", toastStyle(type));

      StackPane root = new StackPane(label);
      root.setPadding(new Insets(12, 14, 12, 14));
      root.setAlignment(Pos.CENTER_LEFT);
      root.getStyleClass().add("toast-shell");

      popup.getContent().add(root);
      popup.setAutoFix(true);
      popup.setAutoHide(true);

      double x = window.getX() + window.getWidth() - 360;
      double y = window.getY() + window.getHeight() - 110;
      popup.show(
        window,
        Math.max(window.getX() + 16, x),
        Math.max(window.getY() + 16, y)
      );

      PauseTransition pause = new PauseTransition(Duration.seconds(3));
      pause.setOnFinished(event -> popup.hide());
      pause.play();
    });
  }

  private static String toastStyle(Type type) {
    return switch (type) {
      case SUCCESS -> "toast-success";
      case ERROR -> "toast-error";
      default -> "toast-info";
    };
  }
}
