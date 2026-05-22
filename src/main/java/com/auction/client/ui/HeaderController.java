package com.auction.client.ui;

import com.auction.client.core.ClientContext;
import javafx.fxml.FXML;

public class HeaderController {

    @FXML
    public void goToGallery() {
        try {
            ClientContext.getInstance().getViewLoader().loadView("gallery.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToMyListings() {
        try {
            ClientContext.getInstance().getViewLoader().loadView("user_dashboard.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLogout() {
        try {
            ClientContext context = ClientContext.getInstance();
            if (context.getRmiProvider() != null && context.getRmiProvider().getService() != null) {
                try { context.getRmiProvider().getService().logout(context.getSessionToken()); } catch (Exception ignored) {}
            }
            context.clearSession();
            context.getViewLoader().loadView("login.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
