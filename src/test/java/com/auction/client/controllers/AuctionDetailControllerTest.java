package com.auction.client.controllers;

import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionDetailControllerTest {

    @Test
    public void testReconnectBannerShowHide() throws Exception {
        // Initialize JavaFX toolkit
        new JFXPanel();

        AuctionDetailController ctrl = new AuctionDetailController();
        HBox banner = new HBox();
        Label lbl = new Label();
        // use reflection to set private fields
        java.lang.reflect.Field fb = AuctionDetailController.class.getDeclaredField("reconnectBanner");
        fb.setAccessible(true);
        fb.set(ctrl, banner);
        java.lang.reflect.Field fl = AuctionDetailController.class.getDeclaredField("reconnectLabel");
        fl.setAccessible(true);
        fl.set(ctrl, lbl);

        // Ensure initial hidden state for the test
        banner.setVisible(false);
        banner.setManaged(false);
        assertFalse(banner.isVisible());

        // Show banner
        ctrl.showReconnectBanner(new RuntimeException("test-fail"));
        // allow FX thread to process
        Thread.sleep(50);
        assertTrue(banner.isVisible());
        assertEquals("Disconnected — test-fail", lbl.getText());

        // Hide banner
        ctrl.hideReconnectBanner();
        Thread.sleep(20);
        assertFalse(banner.isVisible());
    }
}
