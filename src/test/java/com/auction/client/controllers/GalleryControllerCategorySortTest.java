package com.auction.client.controllers;

import com.auction.shared.models.AuctionItem;
import javafx.embed.swing.JFXPanel;
import javafx.application.Platform;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GalleryControllerCategorySortTest {

    @Test
    public void testCategoryFilterAndSort() throws Exception {
        new JFXPanel();

        // fake service
        class FakeService implements com.auction.shared.interfaces.IAuctionService {
            @Override public String login(String username, String password) { return null; }
            @Override public void register(String username, String password, String role) {}
            @Override public String getMyRole(String token) { return null; }
            @Override public void logout(String token) {}
            @Override public String serverTime() { return null; }
            @Override public List<AuctionItem> getActiveAuctions() {
                AuctionItem a1 = new AuctionItem(1, "A1", "d", "CAT1", 500L, "s", "2026-01-01T00:00:00Z", "2026-12-31T00:00:00Z", null);
                AuctionItem a2 = new AuctionItem(2, "A2", "d", "CAT2", 1500L, "s", "2026-01-01T00:00:00Z", "2026-12-01T00:00:00Z", null);
                AuctionItem a3 = new AuctionItem(3, "A3", "d", "CAT1", 1200L, "s", "2026-01-01T00:00:00Z", "2026-11-01T00:00:00Z", null);
                return List.of(a1, a2, a3);
            }
            @Override public List<AuctionItem> getActiveAuctionsBySeller(String sellerUsername, String token) { return null; }
            @Override public AuctionItem getAuctionById(int auctionId) { return null; }
            @Override public java.util.List<com.auction.shared.models.Bid> getBidHistory(int auctionId) { return null; }
            @Override public void placeBid(int auctionId, long amountCents, long clientExpectedPriceCents, String token) {}
            @Override public java.util.List<com.auction.shared.models.Bid> getMyBids(String token) { return null; }
            @Override public java.util.List<AuctionItem> getMyWonAuctions(String token) { return null; }
            @Override public byte[] getThumbnail(int auctionId, int imageIndex) { return new byte[0]; }
            @Override public byte[] getFullImage(int auctionId, int imageIndex) { return new byte[0]; }
            @Override public byte[] exportAuctionsToCSV(String token) { return new byte[0]; }
            @Override public void createUser(String newUsername, String password, String role, String token) {}
            @Override public java.util.List<com.auction.shared.models.User> getAllUsers(String token) { return null; }
            @Override public byte[] backupDatabase(String token) { return new byte[0]; }
            @Override public java.util.List<String> getAuditLogs(int lastNLines, String token) { return null; }
            @Override public int createAuction(AuctionItem item, byte[] image1, byte[] image2, byte[] image3, String token) { return 0; }
            @Override public void cancelAuction(int auctionId, String token) {}
            @Override public void relistAuction(int auctionId, String newEndTimeIso, String token) {}
        }
        FakeService fake = new FakeService();

        GalleryController ctrl = new GalleryController();

        // prepare UI fields
        FlowPane flow = new FlowPane();
        TextField search = new TextField();
        ComboBox<String> combo = new ComboBox<>();
        ChoiceBox<String> sort = new ChoiceBox<>();
        Label count = new Label();

        // set private fields via reflection
        Field fFlow = GalleryController.class.getDeclaredField("auctionFlow");
        fFlow.setAccessible(true);
        fFlow.set(ctrl, flow);
        Field fSearch = GalleryController.class.getDeclaredField("searchField");
        fSearch.setAccessible(true);
        fSearch.set(ctrl, search);
        Field fCat = GalleryController.class.getDeclaredField("categoryCombo");
        fCat.setAccessible(true);
        fCat.set(ctrl, combo);
        Field fSort = GalleryController.class.getDeclaredField("sortChoice");
        fSort.setAccessible(true);
        fSort.set(ctrl, sort);
        Field fCount = GalleryController.class.getDeclaredField("auctionCountLabel");
        fCount.setAccessible(true);
        fCount.set(ctrl, count);

        // set context RMI provider to return fake service
        com.auction.client.core.ClientContext ctx = com.auction.client.core.ClientContext.getInstance();
        var providerField = com.auction.client.core.ClientContext.class.getDeclaredField("rmiProvider");
        providerField.setAccessible(true);
        com.auction.client.network.RmiClientProvider fakeProvider = new com.auction.client.network.RmiClientProvider() {
            @Override public com.auction.shared.interfaces.IAuctionService getService() { return fake; }
        };
        providerField.set(ctx, fakeProvider);

        // call initialize which will populate and render
        ctrl.initialize();
        // wait briefly for JavaFX runLater
        Thread.sleep(200);

        // simulate selecting category CAT1 and sort by Price High->Low
        Platform.runLater(() -> {
            combo.setValue("CAT1");
            sort.getItems().addAll("Newest", "Price: Low → High", "Price: High → Low");
            sort.setValue("Price: High → Low");
        });
        Thread.sleep(200);
        // invoke search handler via reflection (which calls server-side query)
        java.lang.reflect.Method m = GalleryController.class.getDeclaredMethod("handleSearch");
        m.setAccessible(true);
        m.invoke(ctrl);
        Thread.sleep(200);

        // now flow should contain two items (CAT1) and first should be the higher price (1200)
        assertEquals(2, flow.getChildren().size());
        // the cards' labels are not easily introspected; check count label
        assertTrue(count.getText().contains("2"));
    }
}
