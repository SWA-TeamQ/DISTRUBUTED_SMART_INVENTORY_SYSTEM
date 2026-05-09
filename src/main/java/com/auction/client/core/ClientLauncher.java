package com.auction.client.core;

import com.auction.client.ClientApp;
import javafx.application.Application;

/**
 * Client entry point. Launches the JavaFX Application.
 * Separate from ClientApp to avoid module system issues with Application subclass.
 * Run: mvn javafx:run
 */
public class ClientLauncher {
    public static void main(String[] args) {
        Application.launch(ClientApp.class, args);
    }
}
