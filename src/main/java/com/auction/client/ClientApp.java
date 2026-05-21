package com.auction.client;

import com.auction.client.ui.ClientNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

import atlantafx.base.theme.PrimerDark;

/**
 * JavaFX Application entry point.
 * Loads the Connect screen first, then proceeds through the flow:
 * Connect -> Login -> Role-appropriate views.
 */
public class ClientApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        com.auction.client.ui.ViewLoader loader = new com.auction.client.ui.ViewLoader(primaryStage);
        // Ensure both navigator and client context know about the view loader
        ClientNavigator.setViewLoader(loader);
        com.auction.client.core.ClientContext.getInstance().setViewLoader(loader);

        // Smart startup: if running in mock mode, start at the gallery; otherwise
        // start at the connect screen which continues the normal flow to login.
        if (Boolean.parseBoolean(System.getProperty("rtdas.mockMode", "true"))) {
            loader.loadView("gallery.fxml");
        } else {
            loader.loadView("connect.fxml");
        }
        primaryStage.setTitle("RTDAS - Real-Time Distributed Auction System");
        primaryStage.setWidth(800);
        primaryStage.setHeight(600);
        primaryStage.show();
    }

    @Override
    public void stop() {
        // TODO: cleanup polling threads, close RMI connections
    }
}
