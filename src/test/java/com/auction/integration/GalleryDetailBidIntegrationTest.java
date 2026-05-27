package com.auction.integration;

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
import com.auction.shared.models.AuctionItem;
import com.auction.shared.models.Bid;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GalleryDetailBidIntegrationTest {

    private DatabaseManager dbManager;
    private com.auction.server.service.AuctionServiceImpl service;
    private java.nio.file.Path tempDbPath;

    @BeforeEach
    void setUp() throws Exception {
        tempDbPath = java.nio.file.Files.createTempFile("rtdas-integration-", ".sqlite");
        dbManager = new DatabaseManager("jdbc:sqlite:" + tempDbPath.toAbsolutePath());

        UserRepository userRepo = new UserRepository(dbManager.getConnection());
        AuctionRepository auctionRepo = new AuctionRepository(dbManager.getConnection());
        BidRepository bidRepo = new BidRepository(dbManager.getConnection());

        com.auction.server.core.TransactionManager txManager = new com.auction.server.core.TransactionManager(dbManager.getConnection());
        com.auction.server.core.LockManager lockManager = new com.auction.server.core.LockManager();

        com.auction.server.core.AuctionManager auctionManager = new com.auction.server.core.AuctionManager(auctionRepo, bidRepo, lockManager, txManager);
        com.auction.server.core.LifecycleManager lifecycleManager = new com.auction.server.core.LifecycleManager(auctionRepo, bidRepo, lockManager, txManager);
        com.auction.server.core.ImageStore imageStore = new com.auction.server.core.ImageStore(auctionRepo);
        
        com.auction.server.core.SessionManager sessionManager = new com.auction.server.core.SessionManager(userRepo);
        com.auction.server.core.AdminManager adminManager = new com.auction.server.core.AdminManager(auctionManager, userRepo);

        service = new com.auction.server.service.AuctionServiceImpl(auctionManager, sessionManager, adminManager, imageStore);
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) {
            dbManager.close();
        }
        if (tempDbPath != null) {
            try {
                java.nio.file.Files.deleteIfExists(tempDbPath);
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    void galleryToDetailShouldReturnCreatedAuction() throws Exception {
        service.register("seller_int_1", "pw", Constants.USER);
        String sellerToken = service.login("seller_int_1", "pw");

        AuctionItem create = new AuctionItem();
        create.setTitle("Integration Camera");
        create.setDescription("Camera used for integration path test");
        create.setCategory("Electronics");
        create.setStartingPriceCents(10_000);
        create.setCurrentBidCents(10_000);
        create.setStartTime(Instant.now().toString());
        create.setEndTime(Instant.now().plusSeconds(3600).toString());

        int auctionId = service.createAuction(create, null, null, null, sellerToken);
        assertTrue(auctionId > 0);

        List<AuctionItem> active = service.getActiveAuctions();
        assertFalse(active.isEmpty());
        assertTrue(active.stream().anyMatch(a -> a.getId() == auctionId));

        AuctionItem detail = service.getAuctionById(auctionId);
        assertNotNull(detail);
        assertEquals("Integration Camera", detail.getTitle());
        assertEquals("Camera used for integration path test", detail.getDescription());
        assertEquals(10_000, detail.getCurrentBidCents());
    }

    @Test
    void bidFlowShouldUpdateDetailAndHistory() throws Exception {
        service.register("seller_int_2", "pw", Constants.USER);
        service.register("bidder_int_2", "pw", Constants.USER);

        String sellerToken = service.login("seller_int_2", "pw");
        String bidderToken = service.login("bidder_int_2", "pw");

        AuctionItem create = new AuctionItem();
        create.setTitle("Integration Guitar");
        create.setDescription("Guitar bid integration test");
        create.setCategory("Music");
        create.setStartingPriceCents(20_000);
        create.setCurrentBidCents(20_000);
        create.setStartTime(Instant.now().toString());
        create.setEndTime(Instant.now().plusSeconds(3600).toString());

        int auctionId = service.createAuction(create, null, null, null, sellerToken);

        AuctionItem beforeBid = service.getAuctionById(auctionId);
        assertEquals(20_000, beforeBid.getCurrentBidCents());

        long bidAmount = 21_000;
        service.placeBid(auctionId, bidAmount, beforeBid.getCurrentBidCents(), bidderToken);

        AuctionItem afterBid = service.getAuctionById(auctionId);
        assertEquals(bidAmount, afterBid.getCurrentBidCents());
        assertEquals("bidder_int_2", afterBid.getHighestBidderUsername());

        List<Bid> history = service.getBidHistory(auctionId);
        assertEquals(1, history.size());
        assertEquals(bidAmount, history.get(0).getAmountCents());
        assertEquals("bidder_int_2", history.get(0).getBidderUsername());
    }
}
