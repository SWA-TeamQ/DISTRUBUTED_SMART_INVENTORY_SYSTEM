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
    private java.nio.file.Path tempDbPath;

    @BeforeEach
    void setUp() throws Exception {
        tempDbPath = java.nio.file.Files.createTempFile("rtdas-auction-repo-", ".sqlite");
        dbManager = new DatabaseManager("jdbc:sqlite:" + tempDbPath.toAbsolutePath());
        userRepo = new UserRepository(dbManager.getConnection());
        auctionRepo = new AuctionRepository(dbManager.getConnection());
        
        // Ensure a seller exists
        userRepo.insertUser("seller1", "hash", Constants.USER);
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

    @Test
    void shouldSearchActiveAuctionsByQueryCategoryAndSort() {
        AuctionItem a1 = new AuctionItem();
        a1.setTitle("Gaming Laptop");
        a1.setDescription("High-end GPU");
        a1.setCategory("Electronics");
        a1.setStartingPriceCents(100000);
        a1.setCurrentBidCents(120000);
        a1.setSellerUsername("seller1");
        a1.setStartTime("2026-05-01T10:00:00Z");
        a1.setEndTime("2026-05-11T10:00:00Z");
        a1.setCapEndTime("2026-05-11T10:10:00Z");
        a1.setStatus("ACTIVE");
        auctionRepo.insertAuction(a1);

        AuctionItem a2 = new AuctionItem();
        a2.setTitle("Office Chair");
        a2.setDescription("Ergonomic chair");
        a2.setCategory("Furniture");
        a2.setStartingPriceCents(10000);
        a2.setCurrentBidCents(15000);
        a2.setSellerUsername("seller1");
        a2.setStartTime("2026-05-01T10:00:00Z");
        a2.setEndTime("2026-05-09T10:00:00Z");
        a2.setCapEndTime("2026-05-09T10:10:00Z");
        a2.setStatus("ACTIVE");
        auctionRepo.insertAuction(a2);

        AuctionItem a3 = new AuctionItem();
        a3.setTitle("Gaming Mouse");
        a3.setDescription("RGB mouse");
        a3.setCategory("Electronics");
        a3.setStartingPriceCents(2000);
        a3.setCurrentBidCents(3000);
        a3.setSellerUsername("seller1");
        a3.setStartTime("2026-05-01T10:00:00Z");
        a3.setEndTime("2026-05-08T10:00:00Z");
        a3.setCapEndTime("2026-05-08T10:10:00Z");
        a3.setStatus("ACTIVE");
        auctionRepo.insertAuction(a3);

        List<AuctionItem> q = auctionRepo.searchActiveAuctions("gaming", "Electronics", "price_desc");
        assertEquals(2, q.size());
        assertTrue(q.get(0).getCurrentBidCents() >= q.get(1).getCurrentBidCents());

        List<AuctionItem> lowToHigh = auctionRepo.searchActiveAuctions(null, null, "price_asc");
        assertEquals(3, lowToHigh.size());
        assertTrue(lowToHigh.get(0).getCurrentBidCents() <= lowToHigh.get(1).getCurrentBidCents());
        assertTrue(lowToHigh.get(1).getCurrentBidCents() <= lowToHigh.get(2).getCurrentBidCents());
    }
}
