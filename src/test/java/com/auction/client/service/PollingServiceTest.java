package com.auction.client.service;

import com.auction.shared.interfaces.IAuctionService;
import com.auction.shared.models.AuctionItem;
import org.junit.jupiter.api.Test;

import java.rmi.RemoteException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class PollingServiceTest {

    @Test
    public void testFailureNotificationAndRecovery() throws Exception {
        final AtomicInteger calls = new AtomicInteger(0);
        IAuctionService fake = new IAuctionService() {
            @Override public String login(String username, String password) { return null; }
            @Override public void register(String username, String password, String role) {}
            @Override public String getMyRole(String token) { return null; }
            @Override public void logout(String token) {}
            @Override public String serverTime() { return null; }
            @Override public java.util.List<com.auction.shared.models.AuctionItem> getActiveAuctions() { return null; }
            @Override public java.util.List<com.auction.shared.models.AuctionItem> getAllAuctions() { return java.util.List.of(); }
            @Override public java.util.List<com.auction.shared.models.AuctionItem> getActiveAuctionsBySeller(String sellerUsername, String token) { return null; }
            @Override
            public AuctionItem getAuctionById(int auctionId) throws RemoteException {
                int c = calls.incrementAndGet();
                if (c <= 2) {
                    throw new RemoteException("transient");
                }
                // return a simple AuctionItem on success
                return new AuctionItem(auctionId, "Title", "Desc", "CAT", 1000L, "seller", "2026-01-01T00:00:00Z", "2026-12-31T00:00:00Z", null);
            }
            @Override public void placeBid(int auctionId, long amountCents, long clientExpectedPriceCents, String token) {}
            @Override public java.util.List<com.auction.shared.models.Bid> getBidHistory(int auctionId) { return null; }
            @Override public int createAuction(com.auction.shared.models.AuctionItem item, byte[] image1, byte[] image2, byte[] image3, String token) { return 0; }
            @Override public void cancelAuction(int auctionId, String token) {}
            @Override public void relistAuction(int auctionId, String newEndTimeIso, String token) {}
            @Override public void startAuction(int auctionId, String token) {}
            @Override public java.util.List<com.auction.shared.models.Bid> getMyBids(String token) { return null; }
            @Override public java.util.List<com.auction.shared.models.AuctionItem> getMyWonAuctions(String token) { return null; }
            @Override public byte[] getThumbnail(int auctionId, int imageIndex) { return new byte[0]; }
            @Override public byte[] getFullImage(int auctionId, int imageIndex) { return new byte[0]; }
            @Override public byte[] exportAuctionsToCSV(String token) { return new byte[0]; }
            @Override public void createUser(String newUsername, String password, String role, String token) {}
            @Override public java.util.List<com.auction.shared.models.User> getAllUsers(String token) { return null; }
            @Override public byte[] backupDatabase(String token) { return new byte[0]; }
            @Override public java.util.List<String> getAuditLogs(int lastNLines, String token) { return null; }
        };

        PollingService ps = new PollingService(fake, 1, 2, 4);

        CountDownLatch failureLatch = new CountDownLatch(1);
        CountDownLatch updateLatch = new CountDownLatch(1);

        Consumer<AuctionItem> onUpdate = item -> {
            updateLatch.countDown();
        };
        Consumer<Throwable> onFailure = t -> {
            failureLatch.countDown();
        };

        ps.startPolling(42, onUpdate, onFailure);

        // Wait for failure notification (after 2 consecutive failures)
        boolean notified = failureLatch.await(6, TimeUnit.SECONDS);
        assertTrue(notified, "Expected failure notification after consecutive errors");

        // Then wait for a successful update
        boolean updated = updateLatch.await(8, TimeUnit.SECONDS);
        assertTrue(updated, "Expected an update after recovery from transient errors");

        ps.shutdown();
    }
}
