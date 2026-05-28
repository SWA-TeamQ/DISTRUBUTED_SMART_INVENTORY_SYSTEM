package com.auction.client.controllers;

import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UserDashboardFxmlLoadTest {

    @Test
    void userDashboardFxmlShouldLoadSuccessfully() throws Exception {
        new JFXPanel();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user_dashboard.fxml"));
        Parent root = loader.load();
        assertNotNull(root);
        assertNotNull(loader.getController());
    }
}
