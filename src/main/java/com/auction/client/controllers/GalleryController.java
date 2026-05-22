package com.auction.client.controllers;

import javafx.fxml.FXML;

public class GalleryController {

    @FXML
    public void initialize() {
        // TODO: load active auctions, display in grid/list, fetch thumbnails
    }

    @FXML
    private void handleBackToDashboard() {
        try {
            com.auction.client.core.ClientContext.getInstance().getViewLoader().loadView("seller_dashboard.fxml");
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
}
