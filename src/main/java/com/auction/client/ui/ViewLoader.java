package com.auction.client.ui;

import com.auction.client.controllers.AuctionDetailController;
import com.auction.client.core.ClientContext;
import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Loads FXML views and switches scenes on the primary stage.
 * FXML files are loaded from /fxml/ resources directory.
 */
public class ViewLoader {

  private final Stage primaryStage;

  public ViewLoader(Stage primaryStage) {
    this.primaryStage = primaryStage;
  }

  /**
   * Load an FXML view and switch the stage's scene.
   * @param fxmlFile filename (e.g., "login.fxml")
   * @return the controller instance for the loaded view
   */
  public <T> T loadView(String fxmlFile) throws IOException {
    URL viewUrl = getClass().getResource("/fxml/" + fxmlFile);

    // Tolerate dash/underscore naming mismatches for safer runtime navigation.
    if (viewUrl == null) {
      String alternate = fxmlFile.contains("_")
        ? fxmlFile.replace('_', '-')
        : fxmlFile.replace('-', '_');
      viewUrl = getClass().getResource("/fxml/" + alternate);
    }

    if (viewUrl == null) {
      throw new IOException("View not found: " + fxmlFile);
    }

    FXMLLoader loader = new FXMLLoader(viewUrl);
    Parent root = loader.load();
    Scene scene = new Scene(root);
    scene
      .getStylesheets()
      .add(getClass().getResource("/css/style.css").toExternalForm());
    if (fxmlFile.contains("admin")) {
      scene
        .getStylesheets()
        .add(getClass().getResource("/css/admin-panel.css").toExternalForm());
    }
    primaryStage.setScene(scene);
    // If an AuctionDetailController was loaded, ensure it gets a deterministic return target
    Object ctrl = loader.getController();
    try {
      if (ctrl instanceof AuctionDetailController adc) {
        String prev = ClientContext.getInstance().getPreviousViewName();
        if (prev != null && !prev.isBlank()) {
          adc.setReturnViewName(prev);
        }
      }
    } catch (Exception ignored) {}
    return loader.getController();
  }

  public Stage getStage() {
    return primaryStage;
  }
}
