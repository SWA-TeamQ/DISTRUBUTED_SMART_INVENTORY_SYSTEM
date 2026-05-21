package com.auction.client.ui;

import java.io.IOException;

/**
 * Small navigation helper so controllers can swap scenes without knowing about the stage.
 */
public final class ClientNavigator {

    private static ViewLoader viewLoader;

    private ClientNavigator() {}

    public static void setViewLoader(ViewLoader loader) {
        viewLoader = loader;
    }

    public static <T> T loadView(String fxmlFile) throws IOException {
        if (viewLoader == null) {
            throw new IllegalStateException("ViewLoader has not been initialized");
        }
        return viewLoader.loadView(fxmlFile);
    }
}