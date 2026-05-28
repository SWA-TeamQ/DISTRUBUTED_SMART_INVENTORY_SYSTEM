package com.auction.client.controllers;

import com.auction.shared.models.Bid;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionDetailBidHistoryTest {

    @Test
    public void testRefreshBidHistoryPopulatesTable() throws Exception {
        new JFXPanel();

        // fake service that returns a simple bid list
        class FakeService implements com.auction.shared.interfaces.IAuctionService {
            @Override public String login(String username, String password) { return null; }
            @Override public void register(String username, String password, String role) {}
            @Override public String getMyRole(String token) { return null; }
            @Override public void logout(String token) {}
            @Override public String serverTime() { return null; }
            @Override public java.util.List<com.auction.shared.models.AuctionItem> getActiveAuctions() { return null; }
            @Override public java.util.List<com.auction.shared.models.AuctionItem> getAllAuctions() { return java.util.List.of(); }
            @Override public java.util.List<com.auction.shared.models.AuctionItem> getActiveAuctionsBySeller(String sellerUsername, String token) { return null; }
            @Override public com.auction.shared.models.AuctionItem getAuctionById(int auctionId) { return null; }
            @Override
            public java.util.List<Bid> getBidHistory(int auctionId) {
                return List.of(new Bid(1, auctionId, "alice", 1500L, "2026-05-25T12:00:00Z"));
            }
            @Override public void placeBid(int auctionId, long amountCents, long clientExpectedPriceCents, String token) {}
            @Override public java.util.List<com.auction.shared.models.Bid> getMyBids(String token) { return null; }
            @Override public java.util.List<com.auction.shared.models.AuctionItem> getMyWonAuctions(String token) { return null; }
            @Override public byte[] getThumbnail(int auctionId, int imageIndex) { return new byte[0]; }
            @Override public byte[] getFullImage(int auctionId, int imageIndex) { return new byte[0]; }
            @Override public byte[] exportAuctionsToCSV(String token) { return new byte[0]; }
            @Override public void createUser(String newUsername, String password, String role, String token) {}
            @Override public java.util.List<com.auction.shared.models.User> getAllUsers(String token) { return null; }
            @Override public byte[] backupDatabase(String token) { return new byte[0]; }
            @Override public java.util.List<String> getAuditLogs(int lastNLines, String token) { return null; }
            @Override public int createAuction(com.auction.shared.models.AuctionItem item, byte[] image1, byte[] image2, byte[] image3, String token) { return 0; }
            @Override public void cancelAuction(int auctionId, String token) {}
            @Override public void relistAuction(int auctionId, String newEndTimeIso, String token) {}
            @Override public void startAuction(int auctionId, String token) {}
        }
        FakeService fake = new FakeService();

        AuctionDetailController ctrl = new AuctionDetailController();
        TableView<Bid> table = new TableView<>();
        TableColumn<Bid, String> timeCol = new TableColumn<>();
        TableColumn<Bid, String> userCol = new TableColumn<>();
        TableColumn<Bid, Long> amountCol = new TableColumn<>();

        // set private fields via reflection
        java.lang.reflect.Field fb = AuctionDetailController.class.getDeclaredField("bidHistoryTable");
        fb.setAccessible(true);
        fb.set(ctrl, table);
        java.lang.reflect.Field ft = AuctionDetailController.class.getDeclaredField("timeColumn");
        ft.setAccessible(true);
        ft.set(ctrl, timeCol);
        java.lang.reflect.Field fu = AuctionDetailController.class.getDeclaredField("userColumn");
        fu.setAccessible(true);
        fu.set(ctrl, userCol);
        java.lang.reflect.Field fa = AuctionDetailController.class.getDeclaredField("amountColumn");
        fa.setAccessible(true);
        fa.set(ctrl, amountCol);

        // set context RMI provider to return fake service
        com.auction.client.core.ClientContext ctx = com.auction.client.core.ClientContext.getInstance();
        // set RmiProvider via reflection to return fake service
        var providerField = com.auction.client.core.ClientContext.class.getDeclaredField("rmiProvider");
        providerField.setAccessible(true);
        com.auction.client.network.RmiClientProvider fakeProvider = new com.auction.client.network.RmiClientProvider() {
            @Override public com.auction.shared.interfaces.IAuctionService getService() { return fake; }
        };
        providerField.set(ctx, fakeProvider);

        // set current auction id
        java.lang.reflect.Field cid = AuctionDetailController.class.getDeclaredField("currentAuctionId");
        cid.setAccessible(true);
        cid.setInt(ctrl, 77);

        // run refresh via reflection (method is private)
        java.lang.reflect.Method m = AuctionDetailController.class.getDeclaredMethod("refreshBidHistory");
        m.setAccessible(true);
        m.invoke(ctrl);
        // wait briefly for async task to finish
        Thread.sleep(200);
        assertNotNull(table.getItems());
        assertEquals(1, table.getItems().size());
        assertEquals("alice", table.getItems().get(0).getBidderUsername());
    }
}
