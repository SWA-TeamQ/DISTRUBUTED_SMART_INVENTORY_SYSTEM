package com.auction.client;

import com.auction.client.core.ClientContext;
import com.auction.client.service.ThumbnailExecutor;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.stage.Stage;

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
        ClientContext.getInstance().setViewLoader(loader);
        loader.loadView("connect.fxml");
        primaryStage.setTitle("RTDAS - Real-Time Distributed Auction System");
        primaryStage.setWidth(800);
        primaryStage.setHeight(600);
        primaryStage.show();
    }

    @Override
    public void stop() {
        ClientContext.getInstance().shutdown();
        ThumbnailExecutor.shutdown();
    }
}
