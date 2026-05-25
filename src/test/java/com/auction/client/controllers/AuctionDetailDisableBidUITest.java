package com.auction.client.controllers;

import com.auction.shared.models.AuctionItem;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionDetailDisableBidUITest {

    @Test
    public void testBidUiDisabledWhenNotActive() throws Exception {
        new JFXPanel();

        AuctionDetailController ctrl = new AuctionDetailController();
        Button placeBtn = new Button();
        TextField amt = new TextField();
        Label status = new Label();
        Label title = new Label();
        Label desc = new Label();
        Label currentBid = new Label();
        Label highest = new Label();
        Label timeLeft = new Label();

        java.lang.reflect.Field fp = AuctionDetailController.class.getDeclaredField("placeBidButton");
        fp.setAccessible(true);
        fp.set(ctrl, placeBtn);
        java.lang.reflect.Field fa = AuctionDetailController.class.getDeclaredField("bidAmountField");
        fa.setAccessible(true);
        fa.set(ctrl, amt);
        java.lang.reflect.Field fs = AuctionDetailController.class.getDeclaredField("bidStatusLabel");
        fs.setAccessible(true);
        fs.set(ctrl, status);
        java.lang.reflect.Field ft = AuctionDetailController.class.getDeclaredField("auctionTitleLabel");
        ft.setAccessible(true);
        ft.set(ctrl, title);
        java.lang.reflect.Field fd = AuctionDetailController.class.getDeclaredField("auctionDescriptionLabel");
        fd.setAccessible(true);
        fd.set(ctrl, desc);
        java.lang.reflect.Field fcb = AuctionDetailController.class.getDeclaredField("currentBidLabel");
        fcb.setAccessible(true);
        fcb.set(ctrl, currentBid);
        java.lang.reflect.Field fh = AuctionDetailController.class.getDeclaredField("highestBidderLabel");
        fh.setAccessible(true);
        fh.set(ctrl, highest);
        java.lang.reflect.Field ftl = AuctionDetailController.class.getDeclaredField("timeLeftLabel");
        ftl.setAccessible(true);
        ftl.set(ctrl, timeLeft);

        AuctionItem itemClosed = new AuctionItem(1, "x", "d", "CAT", 1000L, "seller", "2026-01-01T00:00:00Z", "2026-01-02T00:00:00Z", null);
        itemClosed.setStatus(com.auction.shared.Constants.STATUS_SOLD);

        java.lang.reflect.Method m = AuctionDetailController.class.getDeclaredMethod("updateUi", AuctionItem.class);
        m.setAccessible(true);
        m.invoke(ctrl, itemClosed);

        assertTrue(placeBtn.isDisable());
        assertTrue(amt.isDisable());
        assertEquals("Bidding closed", status.getText());

        // now set to ACTIVE
        AuctionItem itemActive = new AuctionItem(1, "x", "d", "CAT", 1000L, "seller", "2026-01-01T00:00:00Z", "2027-01-02T00:00:00Z", null);
        itemActive.setStatus(com.auction.shared.Constants.STATUS_ACTIVE);
        m.invoke(ctrl, itemActive);
        assertFalse(placeBtn.isDisable());
        assertFalse(amt.isDisable());
        assertTrue(status.getText().isEmpty() || !status.getText().contains("Bidding closed"));
    }
}
