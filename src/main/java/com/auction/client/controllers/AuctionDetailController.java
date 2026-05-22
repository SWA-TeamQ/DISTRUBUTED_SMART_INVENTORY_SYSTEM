package com.auction.client.controllers;

import javafx.fxml.FXML;

public class AuctionDetailController {

    @FXML
    public void initialize() {
        // TODO: setup polling for updates, handle bid placement
    }
    
    public void shutdown() {
        // TODO: stop polling thread
    }

    @FXML
    private void handleBackToGallery() {
        try {
            com.auction.client.core.ClientContext.getInstance().getViewLoader().loadView("gallery.fxml");
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
}
