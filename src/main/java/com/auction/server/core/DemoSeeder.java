package com.auction.server.core;

import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.BidRepository;
import com.auction.server.repository.DatabaseManager;
import com.auction.server.repository.UserRepository;
import com.auction.shared.Constants;
import com.auction.shared.models.AuctionItem;
import com.auction.shared.models.Bid;
import com.auction.server.util.SecurityUtil;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * DemoSeeder — Populates the database with test users, auctions, and bids
 * for manual testing and UI validation.
 *
 * Run with: mvn exec:java -Dexec.mainClass=com.auction.server.core.DemoSeeder
 *
 * Creates:
 * - 3 Seller accounts (seller-alice, seller-bob, seller-charlie)
 * - 4 Bidder accounts (bella-247, bidder-dan, bidder-eve, bidder-frank)
 * - 6 Active auctions with various end times (short to test Reaper, long for testing)
 * - Initial bids to simulate activity
 */
public class DemoSeeder {

  public static void main(String[] args) {
    System.out.println("🌱 RTDAS Demo Seeder Starting...");

    try {
      // Initialize database
      DatabaseManager dbManager = new DatabaseManager();
      var connection = dbManager.getConnection();
      var userRepo = new UserRepository(connection);
      var auctionRepo = new AuctionRepository(connection);
      var bidRepo = new BidRepository(connection);

      // Seed users
      seedUsers(userRepo);

      // Seed auctions (various end times for testing Reaper and UI)
      seedAuctions(auctionRepo);

      // Seed bids to simulate activity
      seedBids(bidRepo, auctionRepo);

      System.out.println("✅ Demo seeding complete!");
      System.out.println("\nTest Accounts:");
      System.out.println("  Admin:          admin / admin");
      System.out.println("  Seller Alice:   seller-alice / pass123");
      System.out.println("  Seller Bob:     seller-bob / pass123");
      System.out.println("  Seller Charlie: seller-charlie / pass123");
      System.out.println("\n  Bidder (Main):  bella-247 / pass123");
      System.out.println("  Bidder Dan:     bidder-dan / pass123");
      System.out.println("  Bidder Eve:     bidder-eve / pass123");
      System.out.println("  Bidder Frank:   bidder-frank / pass123");
      System.out.println("\n🎯 Test Scenarios:");
      System.out.println(
        "  1. Login as bella-247, browse gallery → test thumbnail display"
      );
      System.out.println(
        "  2. Click an auction → test detail page & place bid"
      );
      System.out.println("  3. Check 'My Activity' → view bids, won, outbid");
      System.out.println(
        "  4. Login as seller-alice → dashboard shows sold/expired auctions"
      );
      System.out.println("  5. Export auction CSV from seller dashboard");
      System.out.println("  6. Watch auctions expire (Reaper) in real-time");

      connection.close();
    } catch (Exception e) {
      System.err.println("❌ Seeding failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void seedUsers(UserRepository userRepo) throws Exception {
    System.out.println("\n📝 Seeding Users...");

    // Sellers
    createUser(userRepo, "seller-alice", "pass123", Constants.USER);
    createUser(userRepo, "seller-bob", "pass123", Constants.USER);
    createUser(userRepo, "seller-charlie", "pass123", Constants.USER);

    // Bidders
    createUser(userRepo, "bella-247", "pass123", Constants.USER);
    createUser(userRepo, "bidder-dan", "pass123", Constants.USER);
    createUser(userRepo, "bidder-eve", "pass123", Constants.USER);
    createUser(userRepo, "bidder-frank", "pass123", Constants.USER);

    System.out.println("  ✓ 7 users created (3 sellers, 4 bidders)");
  }

  private static void createUser(
    UserRepository userRepo,
    String username,
    String password,
    String role
  ) throws Exception {
    String hashedPassword = SecurityUtil.hashPassword(password);
    userRepo.insertUser(username, hashedPassword, role);
  }

  private static void seedAuctions(AuctionRepository auctionRepo)
    throws Exception {
    System.out.println("\n🎨 Seeding Auctions...");

    Instant now = Instant.now();
    int auctionCount = 0;

    // Auction 1: Long-running (24 hours) - electronics category
    AuctionItem a1 = createAuctionTemplate(
      "Vintage Sony Walkman",
      "Classic 1980s portable cassette player. Fully functional.",
      "Electronics",
      1500, // $15.00
      now.plus(Duration.ofHours(24)),
      "seller-alice"
    );
    auctionRepo.insertAuction(a1);
    auctionCount++;

    // Auction 2: Short-running (5 minutes) - FOR REAPER TESTING
    AuctionItem a2 = createAuctionTemplate(
      "Original Atari 2600",
      "Vintage gaming console with 10 games. Great condition.",
      "Electronics",
      5000, // $50.00
      now.plus(Duration.ofMinutes(5)),
      "seller-bob"
    );
    auctionRepo.insertAuction(a2);
    auctionCount++;

    // Auction 3: Medium (4 hours) - furniture
    AuctionItem a3 = createAuctionTemplate(
      "Mid-Century Modern Teak Chair",
      "Authentic 1960s Danish design chair. Minor wear. Beautiful piece.",
      "Furniture",
      8000, // $80.00
      now.plus(Duration.ofHours(4)),
      "seller-alice"
    );
    auctionRepo.insertAuction(a3);
    auctionCount++;

    // Auction 4: Long (48 hours) - art category
    AuctionItem a4 = createAuctionTemplate(
      "Abstract Oil Painting - Untitled",
      "Modern abstract piece. 24x36\". Signed by artist.",
      "Art",
      15000, // $150.00
      now.plus(Duration.ofHours(48)),
      "seller-charlie"
    );
    auctionRepo.insertAuction(a4);
    auctionCount++;

    // Auction 5: Short (3 minutes) - FOR REAPER & SNIPE TESTING
    AuctionItem a5 = createAuctionTemplate(
      "Rare First Edition Harry Potter",
      "Harry Potter and the Philosopher's Stone, First Edition. Pristine condition.",
      "Books",
      20000, // $200.00
      now.plus(Duration.ofMinutes(3)),
      "seller-bob"
    );
    auctionRepo.insertAuction(a5);
    auctionCount++;

    // Auction 6: 12 hours - furniture, good for all-day testing
    AuctionItem a6 = createAuctionTemplate(
      "Ikea Billy Bookshelf (Custom)",
      "5-shelf bookcase, walnut finish. Minimal marks. Fits most rooms.",
      "Furniture",
      2000, // $20.00
      now.plus(Duration.ofHours(12)),
      "seller-alice"
    );
    auctionRepo.insertAuction(a6);
    auctionCount++;

    System.out.println("  ✓ " + auctionCount + " auctions created");
    System.out.println("    - 2 short auctions (5 & 3 min) for Reaper testing");
    System.out.println("    - 2 medium auctions (4 & 12 hours) for UI testing");
    System.out.println(
      "    - 2 long auctions (24 & 48 hours) for full-day testing"
    );
  }

  private static void seedBids(
    BidRepository bidRepo,
    AuctionRepository auctionRepo
  ) throws Exception {
    System.out.println("\n💰 Seeding Bids (simulating bidder activity)...");

    List<AuctionItem> auctions = auctionRepo.findActiveAuctions();
    int bidCount = 0;

    Instant now = Instant.now();

    // Place bids on auction 1 (Walkman) - creates competition
    if (auctions.size() > 0) {
      AuctionItem a1 = auctions.get(0);
      placeBid(
        bidRepo,
        auctionRepo,
        a1.getId(),
        "bella-247",
        a1.getStartingPriceCents() + 500,
        now
      );
      placeBid(
        bidRepo,
        auctionRepo,
        a1.getId(),
        "bidder-dan",
        a1.getStartingPriceCents() + 1000,
        now.plusSeconds(10)
      );
      placeBid(
        bidRepo,
        auctionRepo,
        a1.getId(),
        "bella-247",
        a1.getStartingPriceCents() + 1600,
        now.plusSeconds(20)
      );
      bidCount += 3;
    }

    // Place bids on auction 3 (Chair) - bella-247 leading
    if (auctions.size() > 2) {
      AuctionItem a3 = auctions.get(2);
      placeBid(
        bidRepo,
        auctionRepo,
        a3.getId(),
        "bidder-eve",
        a3.getStartingPriceCents() + 1000,
        now
      );
      placeBid(
        bidRepo,
        auctionRepo,
        a3.getId(),
        "bella-247",
        a3.getStartingPriceCents() + 1800,
        now.plusSeconds(15)
      );
      placeBid(
        bidRepo,
        auctionRepo,
        a3.getId(),
        "bidder-frank",
        a3.getStartingPriceCents() + 2500,
        now.plusSeconds(30)
      );
      placeBid(
        bidRepo,
        auctionRepo,
        a3.getId(),
        "bella-247",
        a3.getStartingPriceCents() + 3200,
        now.plusSeconds(45)
      );
      bidCount += 4;
    }

    // Place bids on auction 4 (Painting) - high-value item
    if (auctions.size() > 3) {
      AuctionItem a4 = auctions.get(3);
      placeBid(
        bidRepo,
        auctionRepo,
        a4.getId(),
        "bidder-dan",
        a4.getStartingPriceCents() + 2000,
        now
      );
      placeBid(
        bidRepo,
        auctionRepo,
        a4.getId(),
        "bella-247",
        a4.getStartingPriceCents() + 3500,
        now.plusSeconds(20)
      );
      placeBid(
        bidRepo,
        auctionRepo,
        a4.getId(),
        "bidder-eve",
        a4.getStartingPriceCents() + 5000,
        now.plusSeconds(40)
      );
      bidCount += 3;
    }

    // Place bids on auction 6 (Bookshelf) - light competition
    if (auctions.size() > 5) {
      AuctionItem a6 = auctions.get(5);
      placeBid(
        bidRepo,
        auctionRepo,
        a6.getId(),
        "bella-247",
        a6.getStartingPriceCents() + 300,
        now
      );
      bidCount += 1;
    }

    System.out.println("  ✓ " + bidCount + " bids placed across auctions");
    System.out.println("    - bella-247 is active bidder on multiple auctions");
    System.out.println("    - Simulates competitive bidding scenarios");
  }

  private static void placeBid(
    BidRepository bidRepo,
    AuctionRepository auctionRepo,
    int auctionId,
    String bidder,
    long amountCents,
    Instant timestamp
  ) throws Exception {
    Bid bid = new Bid();
    bid.setAuctionItemId(auctionId);
    bid.setBidderUsername(bidder);
    bid.setAmountCents(amountCents);
    bid.setTimestamp(timestamp.toString());

    bidRepo.insertBid(bid);

    // Update auction's current bid & highest bidder
    auctionRepo.updateAuctionBid(auctionId, amountCents, bidder);
  }

  private static AuctionItem createAuctionTemplate(
    String title,
    String description,
    String category,
    long startingPriceCents,
    Instant endTime,
    String sellerUsername
  ) {
    AuctionItem item = new AuctionItem();
    item.setTitle(title);
    item.setDescription(description);
    item.setCategory(category);
    item.setStartingPriceCents(startingPriceCents);
    item.setCurrentBidCents(0); // No bids yet
    item.setHighestBidderUsername(null);
    item.setSellerUsername(sellerUsername);

    Instant now = Instant.now();
    item.setStartTime(now.toString());
    item.setEndTime(endTime.toString());

    // Snipe cap = endTime + 10 minutes
    Instant capEndTime = endTime.plus(
      Duration.ofMinutes(Constants.SNIPE_CAP_DEFAULT_MINUTES)
    );
    item.setCapEndTime(capEndTime.toString());

    item.setStatus(Constants.STATUS_ACTIVE);

    // Image paths will be set by seedAuctions after auction is created with ID
    item.setImg1(null);
    item.setImg2(null);
    item.setImg3(null);

    item.setRelistedFrom(null);

    return item;
  }
}
