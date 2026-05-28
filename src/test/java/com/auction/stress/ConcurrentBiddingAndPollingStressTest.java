package com.auction.stress;

import com.auction.client.service.PollingService;
import com.auction.server.core.AdminManager;
import com.auction.server.core.SessionManager;
import com.auction.server.core.AuctionManager;
import com.auction.server.core.ImageStore;
import com.auction.server.core.LifecycleManager;
import com.auction.server.core.LockManager;
import com.auction.server.core.TransactionManager;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.BidRepository;
import com.auction.server.repository.DatabaseManager;
import com.auction.server.repository.UserRepository;
import com.auction.server.service.AuctionServiceImpl;
import com.auction.shared.Constants;
import com.auction.shared.exceptions.AuctionException;
import com.auction.shared.interfaces.IAuctionService;
import com.auction.shared.models.AuctionItem;
import com.auction.shared.models.Bid;
import com.auction.shared.models.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentBiddingAndPollingStressTest {

    private DatabaseManager dbManager;
    private AuctionServiceImpl service;
    private Path tempDbPath;

    @BeforeEach
    void setUp() throws Exception {
        tempDbPath = Files.createTempFile("rtdas-stress-", ".sqlite");
        dbManager = new DatabaseManager("jdbc:sqlite:" + tempDbPath.toAbsolutePath());

        UserRepository userRepo = new UserRepository(dbManager.getConnection());
        AuctionRepository auctionRepo = new AuctionRepository(dbManager.getConnection());
        BidRepository bidRepo = new BidRepository(dbManager.getConnection());

        TransactionManager txManager = new TransactionManager(dbManager.getConnection());
        LockManager lockManager = new LockManager();

        AuctionManager auctionManager = new AuctionManager(auctionRepo, bidRepo, lockManager, txManager);
        LifecycleManager lifecycleManager = new LifecycleManager(auctionRepo, bidRepo, lockManager, txManager);
        ImageStore imageStore = new ImageStore(auctionRepo);
        
        SessionManager sessionManager = new SessionManager(userRepo);
        AdminManager adminManager = new AdminManager(auctionManager, userRepo);

        service = new AuctionServiceImpl(auctionManager, sessionManager, adminManager, imageStore);
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) {
            dbManager.close();
        }
        if (tempDbPath != null) {
            try {
                Files.deleteIfExists(tempDbPath);
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    void concurrentBiddingShouldKeepConsistentHighestBid() throws Exception {
        service.register("seller_stress", "pw", Constants.USER);
        String sellerToken = service.login("seller_stress", "pw");

        int bidders = 2;
        List<String> bidderTokens = new ArrayList<>();
        for (int i = 0; i < bidders; i++) {
            String u = "bidder_stress_" + i;
            service.register(u, "pw", Constants.USER);
            bidderTokens.add(service.login(u, "pw"));
        }

        AuctionItem create = new AuctionItem();
        create.setTitle("Stress Auction");
        create.setDescription("Concurrent bidding stress test");
        create.setCategory("Test");
        create.setStartingPriceCents(10_000);
        create.setCurrentBidCents(10_000);
        create.setStartTime(Instant.now().toString());
        create.setEndTime(Instant.now().plusSeconds(3600).toString());
        int auctionId = service.createAuction(create, null, null, null, sellerToken);

        var pool = Executors.newFixedThreadPool(bidders);
        CountDownLatch done = new CountDownLatch(bidders);

        for (int i = 0; i < bidders; i++) {
            final String token = bidderTokens.get(i);
            final long bump = i + 1;
            pool.submit(() -> {
                try {
                    for (int attempt = 0; attempt < 5; attempt++) {
                        AuctionItem snapshot = service.getAuctionById(auctionId);
                        long current = snapshot.getCurrentBidCents();
                        long minIncrement = (current + 19) / 20;
                        long amount = current + minIncrement + bump;
                        try {
                            service.placeBid(auctionId, amount, current, token);
                        } catch (AuctionException ignored) {
                            // expected under contention; retry with fresh snapshot
                        }
                    }
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        assertTrue(done.await(15, TimeUnit.SECONDS), "Concurrent bid workers did not complete in time");
        pool.shutdownNow();

        AuctionItem finalItem = service.getAuctionById(auctionId);
        List<Bid> history = service.getBidHistory(auctionId);

        assertFalse(history.isEmpty(), "At least one bid should be accepted under contention");
        long maxHistory = history.stream().mapToLong(Bid::getAmountCents).max().orElse(0L);
        assertEquals(maxHistory, finalItem.getCurrentBidCents(), "Final current bid must equal max persisted bid amount");
    }

    @Test
    void pollingServiceShouldStopEmittingAfterShutdown() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger updates = new AtomicInteger();

        IAuctionService fake = new IAuctionService() {
            @Override
            public AuctionItem getAuctionById(int auctionId) {
                calls.incrementAndGet();
                AuctionItem item = new AuctionItem();
                item.setId(auctionId);
                item.setCurrentBidCents(1000);
                item.setTitle("Fake");
                return item;
            }

            @Override public String login(String username, String password) { throw new UnsupportedOperationException(); }
            @Override public void register(String username, String password, String role) { throw new UnsupportedOperationException(); }
            @Override public String getMyRole(String token) { throw new UnsupportedOperationException(); }
            @Override public void logout(String token) { throw new UnsupportedOperationException(); }
            @Override public String serverTime() { throw new UnsupportedOperationException(); }
            @Override public List<AuctionItem> getActiveAuctions() { throw new UnsupportedOperationException(); }
            @Override public List<AuctionItem> getActiveAuctionsBySeller(String sellerUsername, String token) { throw new UnsupportedOperationException(); }
            @Override public List<AuctionItem> getAuctionsBySeller(String sellerUsername, String token) { throw new UnsupportedOperationException(); }
            @Override public void placeBid(int auctionId, long amountCents, long clientExpectedPriceCents, String token) { throw new UnsupportedOperationException(); }
            @Override public List<Bid> getBidHistory(int auctionId) { throw new UnsupportedOperationException(); }
            @Override public int createAuction(AuctionItem item, byte[] image1, byte[] image2, byte[] image3, String token) { throw new UnsupportedOperationException(); }
            @Override public void cancelAuction(int auctionId, String token) { throw new UnsupportedOperationException(); }
            @Override public void relistAuction(int auctionId, String newEndTimeIso, String token) { throw new UnsupportedOperationException(); }
            @Override public List<Bid> getMyBids(String token) { throw new UnsupportedOperationException(); }
            @Override public List<AuctionItem> getMyWonAuctions(String token) { throw new UnsupportedOperationException(); }
            @Override public byte[] getThumbnail(int auctionId, int imageIndex) { throw new UnsupportedOperationException(); }
            @Override public byte[] getFullImage(int auctionId, int imageIndex) { throw new UnsupportedOperationException(); }
            @Override public byte[] exportAuctionsToCSV(String token) { throw new UnsupportedOperationException(); }
            @Override public List<User> getAllUsers(String token) { throw new UnsupportedOperationException(); }
            @Override public List<User> searchUsers(String query, String token) { throw new UnsupportedOperationException(); }
            @Override public void promoteUserToAdmin(String username, String token) { throw new UnsupportedOperationException(); }
            @Override public void demoteUserToStandard(String username, String token) { throw new UnsupportedOperationException(); }
            @Override public List<String> getAuditLogs(int lastNLines, String token) { throw new UnsupportedOperationException(); }
        };

        PollingService polling = new PollingService();
        polling.startPolling(() -> {
            try {
                fake.getAuctionById(1);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
            updates.incrementAndGet();
        }, 1);

        Thread.sleep(2300); // immediate tick + ~1 scheduled tick
        int beforeShutdownCalls = calls.get();
        int beforeShutdownUpdates = updates.get();

        polling.shutdown();

        Thread.sleep(2400); // if leaking, should continue incrementing
        int afterShutdownCalls = calls.get();
        int afterShutdownUpdates = updates.get();

        assertTrue(beforeShutdownCalls >= 1, "Polling should call getAuctionById before shutdown");
        assertTrue(beforeShutdownUpdates >= 1, "Polling should invoke callback before shutdown");
        assertTrue(afterShutdownCalls <= beforeShutdownCalls + 1,
                "Polling calls should stop after shutdown (allowing one in-flight task)");
        assertTrue(afterShutdownUpdates <= beforeShutdownUpdates + 1,
                "Polling callbacks should stop after shutdown (allowing one in-flight task)");
    }
}
