package com.auction.server.core;

import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.BidRepository;
import com.auction.server.repository.UserRepository;
import com.auction.shared.Constants;
import com.auction.shared.models.AuctionItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ScheduledAuctionLifecycleTest {
    private com.auction.server.repository.DatabaseManager dbManager;
    private AuctionRepository auctionRepo;
    private BidRepository bidRepo;
    private UserRepository userRepo;
    private LockManager lockManager;
    private TransactionManager txManager;
    private LifecycleManager lifecycleManager;
    private Path tempDbPath;

    @BeforeEach
    void setUp() throws Exception {
        tempDbPath = Files.createTempFile("rtdas-scheduled-test-", ".sqlite");
        dbManager = new com.auction.server.repository.DatabaseManager("jdbc:sqlite:" + tempDbPath.toAbsolutePath());
        userRepo = new UserRepository(dbManager.getConnection());
        auctionRepo = new AuctionRepository(dbManager.getConnection());
        bidRepo = new BidRepository(dbManager.getConnection());
        lockManager = new LockManager();
        txManager = new TransactionManager(dbManager.getConnection());
        lifecycleManager = new LifecycleManager(auctionRepo, bidRepo, lockManager, txManager);

        // ensure seller exists
        userRepo.insertUser("seller1", "hash", Constants.USER);
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) dbManager.close();
        if (tempDbPath != null) {
            try { Files.deleteIfExists(tempDbPath); } catch (Exception ignored) {}
        }
    }

    @Test
    void scheduledAuctionActivatesAutomatically() {
        AuctionItem item = new AuctionItem();
        item.setTitle("Scheduled Item");
        item.setDescription("Starts immediately");
        item.setCategory("Test");
        item.setStartingPriceCents(1000);
        item.setCurrentBidCents(1000);
        item.setSellerUsername("seller1");
        // start in the past (should activate on sweep)
        item.setStartTime(Instant.now().minusSeconds(1).toString());
        item.setEndTime(Instant.now().plusSeconds(60).toString());
        item.setCapEndTime(null);
        item.setStatus(Constants.STATUS_SCHEDULED);

        int id = auctionRepo.insertAuction(item);
        assertTrue(id > 0);

        lifecycleManager.activateScheduled();

        AuctionItem after = auctionRepo.findAuctionById(id);
        assertNotNull(after);
        assertEquals(Constants.STATUS_ACTIVE, after.getStatus());
    }

    @Test
    void manualStartBySeller() throws Exception {
        AuctionItem item = new AuctionItem();
        item.setTitle("Manual Start Item");
        item.setDescription("Starts later");
        item.setCategory("Test");
        item.setStartingPriceCents(1000);
        item.setCurrentBidCents(1000);
        item.setSellerUsername("seller1");
        item.setStartTime(Instant.now().plusSeconds(3600).toString());
        item.setEndTime(Instant.now().plusSeconds(7200).toString());
        item.setCapEndTime(null);
        item.setStatus(Constants.STATUS_SCHEDULED);

        int id = auctionRepo.insertAuction(item);
        assertTrue(id > 0);

        com.auction.server.core.SessionContext ctx = new com.auction.server.core.SessionContext("seller1", Constants.USER);
        AuctionManager manager = new AuctionManager(auctionRepo, bidRepo, lockManager, txManager);
        manager.startAuction(id, ctx);

        AuctionItem after = auctionRepo.findAuctionById(id);
        assertNotNull(after);
        assertEquals(Constants.STATUS_ACTIVE, after.getStatus());
    }

    @Test
    void concurrentBidsShouldNeverExtendPastCapEndTime() throws Exception {
        userRepo.insertUser("bidder1", "hash", Constants.USER);
        userRepo.insertUser("bidder2", "hash", Constants.USER);
        userRepo.insertUser("bidder3", "hash", Constants.USER);

        AuctionItem item = new AuctionItem();
        item.setTitle("Cap Guard Auction");
        item.setDescription("Concurrent bids near auction end");
        item.setCategory("Test");
        item.setStartingPriceCents(10_000);
        item.setCurrentBidCents(10_000);
        item.setSellerUsername("seller1");
        item.setStartTime(Instant.now().minusSeconds(5).toString());
        Instant originalEnd = Instant.now().plusSeconds(8);
        Instant capEnd = originalEnd.plusSeconds(2);
        item.setEndTime(originalEnd.toString());
        item.setCapEndTime(capEnd.toString());
        item.setStatus(Constants.STATUS_ACTIVE);

        int auctionId = auctionRepo.insertAuction(item);
        assertTrue(auctionId > 0);

        AuctionManager manager = new AuctionManager(auctionRepo, bidRepo, lockManager, txManager);
        SessionContext bidderOne = new SessionContext("bidder1", Constants.USER);
        SessionContext bidderTwo = new SessionContext("bidder2", Constants.USER);
        SessionContext bidderThree = new SessionContext("bidder3", Constants.USER);

        ExecutorService pool = Executors.newFixedThreadPool(3);
        CountDownLatch ready = new CountDownLatch(3);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(3);

        pool.submit(() -> runConcurrentBid(manager, auctionId, bidderOne, ready, start, done));
        pool.submit(() -> runConcurrentBid(manager, auctionId, bidderTwo, ready, start, done));
        pool.submit(() -> runConcurrentBid(manager, auctionId, bidderThree, ready, start, done));

        assertTrue(ready.await(5, TimeUnit.SECONDS), "Bidder threads did not get ready in time");
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "Bidder threads did not finish in time");

        pool.shutdownNow();

        AuctionItem after = auctionRepo.findAuctionById(auctionId);
        assertNotNull(after);
        assertNotNull(after.getCapEndTime());

        Instant finalEnd = Instant.parse(after.getEndTime());
        Instant finalCap = Instant.parse(after.getCapEndTime());

        assertFalse(finalEnd.isAfter(finalCap), "Auction end_time must never exceed cap_end_time");
        assertTrue(finalEnd.equals(finalCap) || finalEnd.isBefore(finalCap),
                "Auction end_time should be at or before cap_end_time");
        assertTrue(finalEnd.isAfter(originalEnd) || finalEnd.equals(originalEnd),
                "Auction end_time should not move backward");
    }

    private void runConcurrentBid(
            AuctionManager manager,
            int auctionId,
            SessionContext bidder,
            CountDownLatch ready,
            CountDownLatch start,
            CountDownLatch done
    ) {
        try {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            AuctionItem snapshot = auctionRepo.findAuctionById(auctionId);
            long expected = snapshot.getCurrentBidCents();
            long bidAmount = expected + Math.max(1L, (expected + 19) / 20);
            try {
                manager.placeBid(auctionId, bidder, bidAmount, expected);
            } catch (Exception ignored) {
                // Contention is expected; this test only cares about the final cap boundary.
            }
        } catch (Exception ignored) {
        } finally {
            done.countDown();
        }
    }
}
