package com.auction.server.repository;

import com.auction.shared.Constants;
import com.auction.shared.models.AuctionItem;
import com.auction.shared.models.Bid;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BidRepositoryTest {

    private DatabaseManager dbManager;
    private UserRepository userRepo;
    private AuctionRepository auctionRepo;
    private BidRepository bidRepo;
    private int testAuctionId;

    @BeforeEach
    void setUp() {
        dbManager = new DatabaseManager("jdbc:sqlite::memory:");
        userRepo = new UserRepository(dbManager.getConnection());
        auctionRepo = new AuctionRepository(dbManager.getConnection());
        bidRepo = new BidRepository(dbManager.getConnection());
        
        userRepo.insertUser("seller1", "hash", Constants.SELLER);
        userRepo.insertUser("bidder1", "hash", Constants.BIDDER);
        
        AuctionItem item = new AuctionItem();
        item.setTitle("Test Item");
        item.setCategory("Test");
        item.setStartingPrice(10.0);
        item.setCurrentBid(10.0);
        item.setSellerUsername("seller1");
        item.setStartTime("2026-05-01T10:00:00Z");
        item.setEndTime("2026-05-10T10:00:00Z");
        item.setStatus("ACTIVE");
        
        testAuctionId = auctionRepo.insertAuction(item);
    }

    @AfterEach
    void tearDown() {
        dbManager.close();
    }

    @Test
    void shouldInsertAndFindBids() {
        Bid bid = new Bid();
        bid.setAuctionItemId(testAuctionId);
        bid.setBidderUsername("bidder1");
        bid.setAmount(15.0);
        bid.setTimestamp("2026-05-02T10:00:00Z");
        
        bidRepo.insertBid(bid);
        assertTrue(bid.getId() > 0, "Should generate a valid bid ID");
        
        List<Bid> bids = bidRepo.findBidsByAuctionId(testAuctionId);
        assertEquals(1, bids.size());
        assertEquals(15.0, bids.get(0).getAmount());
        
        assertEquals(1, bidRepo.countBidsByAuctionId(testAuctionId));
    }
}
