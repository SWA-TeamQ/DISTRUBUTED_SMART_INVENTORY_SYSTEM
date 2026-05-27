package com.auction.client.util;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

public class Toast {

  public static void makeText(
    Stage ownerStage,
    String toastMsg,
    int toastDelay,
    int fadeInDelay,
    int fadeOutDelay
  ) {
    Popup popup = new Popup();
    popup.setAutoFix(true);
    popup.setAutoHide(true);
    popup.setHideOnEscape(true);

    Label label = new Label(toastMsg);
    label.setStyle(
      "-fx-background-color: rgba(30, 30, 30, 0.9); " +
        "-fx-text-fill: white; " +
        "-fx-font-family: 'Segoe UI', sans-serif; " +
        "-fx-font-size: 14px; " +
        "-fx-padding: 10px 20px; " +
        "-fx-background-radius: 5px; " +
        "-fx-border-radius: 5px;"
    );

    StackPane pane = new StackPane(label);
    pane.setStyle(
      "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 10, 0, 0, 5);"
    );

    popup.getContent().add(pane);

    popup.setOnShown(e -> {
      popup.setX(
        ownerStage.getX() + ownerStage.getWidth() / 2 - popup.getWidth() / 2
      );
      popup.setY(ownerStage.getY() + ownerStage.getHeight() - 100);
    });

    // Fade in
    Timeline fadeInTimeline = new Timeline();
    KeyFrame fadeInKey1 = new KeyFrame(
      Duration.ZERO,
      new KeyValue(popup.opacityProperty(), 0.0)
    );
    KeyFrame fadeInKey2 = new KeyFrame(
      Duration.millis(fadeInDelay),
      new KeyValue(popup.opacityProperty(), 1.0)
    );
    fadeInTimeline.getKeyFrames().addAll(fadeInKey1, fadeInKey2);

    // Fade out
    Timeline fadeOutTimeline = new Timeline();
    KeyFrame fadeOutKey1 = new KeyFrame(
      Duration.ZERO,
      new KeyValue(popup.opacityProperty(), 1.0)
    );
    KeyFrame fadeOutKey2 = new KeyFrame(
      Duration.millis(fadeOutDelay),
      new KeyValue(popup.opacityProperty(), 0.0)
    );
    fadeOutTimeline.getKeyFrames().addAll(fadeOutKey1, fadeOutKey2);
    fadeOutTimeline.setOnFinished(ae -> popup.hide());

    // Wait before fade out
    Timeline delayTimeline = new Timeline();
    KeyFrame delayKey = new KeyFrame(Duration.millis(toastDelay));
    delayTimeline.getKeyFrames().add(delayKey);
    delayTimeline.setOnFinished(ae -> fadeOutTimeline.play());

    fadeInTimeline.setOnFinished(ae -> delayTimeline.play());

    popup.setOpacity(0);
    popup.show(ownerStage);
    fadeInTimeline.play();
  }
}
