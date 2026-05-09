package com.auction.client.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

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
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlFile));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        primaryStage.setScene(scene);
        return loader.getController();
    }

    public Stage getStage() {
        return primaryStage;
    }
}
