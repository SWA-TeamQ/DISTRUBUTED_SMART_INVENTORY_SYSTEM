package com.auction.stress;

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
import com.auction.shared.models.AuctionItem;
import com.auction.shared.models.Bid;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ConcurrentBiddingHighStressTest {
    private DatabaseManager dbManager;
    private AuctionServiceImpl service;
    private Path tempDbPath;

    @BeforeEach
    void setUp() throws Exception {
        tempDbPath = Files.createTempFile("rtdas-stress-high-", ".sqlite");
        dbManager = new DatabaseManager("jdbc:sqlite:" + tempDbPath.toAbsolutePath());

        UserRepository userRepo = new UserRepository(dbManager.getConnection());
        AuctionRepository auctionRepo = new AuctionRepository(dbManager.getConnection());
        BidRepository bidRepo = new BidRepository(dbManager.getConnection());

        TransactionManager txManager = new TransactionManager(dbManager.getConnection());
        LockManager lockManager = new LockManager();

        AuctionManager auctionManager = new AuctionManager(auctionRepo, bidRepo, lockManager, txManager);
        LifecycleManager lifecycleManager = new LifecycleManager(auctionRepo, bidRepo, lockManager, txManager);
        ImageStore imageStore = new ImageStore(auctionRepo);

        service = new AuctionServiceImpl(userRepo, auctionManager, lifecycleManager, imageStore);
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) dbManager.close();
        if (tempDbPath != null) {
            try { Files.deleteIfExists(tempDbPath); } catch (Exception ignored) {}
        }
    }

    @Test
    void highConcurrencyBiddingShouldRemainConsistent() throws Exception {
        String adminToken = service.login(Constants.DEFAULT_ADMIN_USERNAME, Constants.DEFAULT_ADMIN_PASSWORD);
        service.createUser("seller_high", "pw", Constants.USER, adminToken);
        String sellerToken = service.login("seller_high", "pw");

        final int bidders = 10;
        // create a small set of tokens and reuse them to avoid tripping login rate limit
        List<String> baseTokens = new ArrayList<>();
        final int distinctTokenCount = Math.min(5, bidders);
        // insert users directly via repository and create session tokens reflectively (avoid rate limiter)
        var ur = new UserRepository(dbManager.getConnection());
        for (int i = 0; i < distinctTokenCount; i++) {
            String u = "bidder_high_base_" + i;
            ur.insertUser(u, com.auction.server.util.SecurityUtil.hashPassword("pw"), Constants.USER);
            // create session token and insert into AuctionServiceImpl.sessions map via reflection
            String token = java.util.UUID.randomUUID().toString();
            Class<?> sessionInfoCls = Class.forName("com.auction.server.service.AuctionServiceImpl$SessionInfo");
            java.lang.reflect.Constructor<?> ctor = sessionInfoCls.getDeclaredConstructor(com.auction.server.core.SessionContext.class, java.time.Instant.class);
            ctor.setAccessible(true);
            com.auction.server.core.SessionContext ctx = new com.auction.server.core.SessionContext(u, Constants.USER);
            Object sessionInfo = ctor.newInstance(ctx, java.time.Instant.now().plus(java.time.Duration.ofMinutes(Constants.SESSION_TTL_MINUTES)));
            java.lang.reflect.Field sessionsField = AuctionServiceImpl.class.getDeclaredField("sessions");
            sessionsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.concurrent.ConcurrentHashMap<String, Object> sessionsMap = (java.util.concurrent.ConcurrentHashMap<String, Object>) sessionsField.get(service);
            sessionsMap.put(token, sessionInfo);
            baseTokens.add(token);
        }
        List<String> bidderTokens = new ArrayList<>();
        for (int i = 0; i < bidders; i++) {
            bidderTokens.add(baseTokens.get(i % baseTokens.size()));
        }

        AuctionItem create = new AuctionItem();
        create.setTitle("High Stress Auction");
        create.setDescription("High concurrency bidding stress test");
        create.setCategory("Stress");
        create.setStartingPriceCents(1000);
        create.setCurrentBidCents(1000);
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
                    for (int attempt = 0; attempt < 20; attempt++) {
                        try {
                            AuctionItem snapshot = service.getAuctionById(auctionId);
                            long current = snapshot.getCurrentBidCents();
                            long minIncrement = (current + 19) / 20;
                            long amount = current + minIncrement + bump;
                            service.placeBid(auctionId, amount, current, token);
                        } catch (AuctionException ignored) {
                            // expected under contention
                        }
                    }
                } catch (Exception ignored) {
                    // swallow remote/SQL exceptions inside worker
                } finally {
                    done.countDown();
                }
            });
        }

        assertTrue(done.await(120, TimeUnit.SECONDS), "High concurrency workers did not finish in time");
        pool.shutdownNow();

        List<Bid> history = service.getBidHistory(auctionId);
        assertFalse(history.isEmpty(), "At least one bid should be accepted under high contention");
        long maxHistory = history.stream().mapToLong(Bid::getAmountCents).max().orElse(0L);
        AuctionItem finalItem = service.getAuctionById(auctionId);
        assertEquals(maxHistory, finalItem.getCurrentBidCents(), "Final current bid must equal max persisted bid amount");
    }
}
