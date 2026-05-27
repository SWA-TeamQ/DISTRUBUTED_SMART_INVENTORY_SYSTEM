package com.auction.client.controllers;

import com.auction.client.core.ClientContext;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ProfileDialogController {

    @FXML private Label usernameLabel;
    @FXML private Label roleLabel;

    @FXML
    public void initialize() {
        ClientContext ctx = ClientContext.getInstance();
        if (ctx != null && ctx.getUsername() != null) {
            usernameLabel.setText(ctx.getUsername());
            // Since we don't have role explicitly accessible directly in context right now, we can default it or check if admin
            roleLabel.setText("User"); 
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) usernameLabel.getScene().getWindow();
        stage.close();
    }
}