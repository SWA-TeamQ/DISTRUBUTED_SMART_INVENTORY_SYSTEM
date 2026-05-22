package com.auction.server.repository;

import com.auction.shared.Constants;
import com.auction.shared.models.AuctionItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuctionRepositoryTest {

    private DatabaseManager dbManager;
    private UserRepository userRepo;
    private AuctionRepository auctionRepo;

    @BeforeEach
    void setUp() {
        dbManager = new DatabaseManager("jdbc:sqlite::memory:");
        userRepo = new UserRepository(dbManager.getConnection());
        auctionRepo = new AuctionRepository(dbManager.getConnection());
        
        // Ensure a user exists for seller-owned auction data
        userRepo.insertUser("seller1", "hash", Constants.USER);
    }

    @AfterEach
    void tearDown() {
        dbManager.close();
    }

    @Test
    void shouldInsertAndFindAuction() {
        AuctionItem item = new AuctionItem();
        item.setTitle("Vintage Lamp");
        item.setDescription("Old lamp");
        item.setCategory("Antiques");
        item.setStartingPriceCents(5000);
        item.setCurrentBidCents(5000);
        item.setSellerUsername("seller1");
        item.setStartTime("2026-05-01T10:00:00Z");
        item.setEndTime("2026-05-10T10:00:00Z");
        item.setCapEndTime("2026-05-10T10:10:00Z");
        item.setStatus("ACTIVE");
        
        int id = auctionRepo.insertAuction(item);
        assertTrue(id > 0, "Should generate a valid ID");
        
        AuctionItem fetched = auctionRepo.findAuctionById(id);
        assertNotNull(fetched);
        assertEquals("Vintage Lamp", fetched.getTitle());
        assertEquals("seller1", fetched.getSellerUsername());
        
        List<AuctionItem> active = auctionRepo.findActiveAuctions();
        assertEquals(1, active.size());
    }
}
