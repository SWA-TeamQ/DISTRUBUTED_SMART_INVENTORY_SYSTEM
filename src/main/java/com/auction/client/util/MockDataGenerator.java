package com.auction.client.util;

import java.time.Duration;
import java.time.Instant;

import com.auction.shared.Constants;
import com.auction.shared.models.AuctionItem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Mock data used as a safe fallback when the live service cannot be reached during demos.
 */
public final class MockDataGenerator {

  private MockDataGenerator() {}

  public static ObservableList<AuctionItem> getMockAuctions() {
    ObservableList<AuctionItem> items = FXCollections.observableArrayList();
    Instant now = Instant.now();

    items.add(
      createAuction(
        101,
        "Handcrafted Walnut Executive Desk with Concealed Drawers and a Shockingly Long Title Intended to Stress the Layout Wrapping Behaviour in Every Surface",
        "A premium walnut desk with hidden cable channels, brass pull handles, and a deliberately overlong description that keeps going to prove the UI can wrap text cleanly without clipping, crashing, or making the gallery card collapse into unreadable mush.",
        "Furniture",
        125000,
        132500,
        "office-buyer",
        "seller-alice",
        Constants.STATUS_ACTIVE,
        now.minus(Duration.ofHours(4)),
        now.plus(Duration.ofHours(16)),
        "auction_1_img_1.jpg",
        "auction_1_img_2.jpg",
        "auction_1_img_3.jpg"
      )
    );

    items.add(
      createAuction(
        102,
        "Limited-Run Studio Headphones",
        "Balanced armature headphones in near-mint condition.",
        "Electronics",
        30000,
        0,
        null,
        "seller-bob",
        Constants.STATUS_ACTIVE,
        now.minus(Duration.ofHours(2)),
        now.plus(Duration.ofHours(8)),
        "auction_2_img_1.jpg",
        "auction_2_img_2.jpg",
        "auction_2_img_3.jpg"
      )
    );

    items.add(
      createAuction(
        103,
        "Massive Bid Artifact with a Value So Large It Exists Primarily to Verify Currency Formatting Across Thousands Separators and Large Magnitudes",
        "This item carries a very large live bid to ensure NumberFormat rendering stays stable even when the amounts are well past normal demo territory.",
        "Collectibles",
        250000000,
        275000000,
        "collector-max",
        "seller-charlie",
        Constants.STATUS_ACTIVE,
        now.minus(Duration.ofHours(1)),
        now.plus(Duration.ofDays(1)),
        "auction_3_img_1.jpg",
        "auction_3_img_2.jpg",
        "auction_3_img_3.jpg"
      )
    );

    items.add(
      createAuction(
        104,
        "Gallery Piece With No Images",
        "Empty image lists are intentional here so the gallery and detail views must fall back to the shared placeholder treatment.",
        "Art",
        45000,
        45000,
        null,
        "seller-dana",
        Constants.STATUS_CANCELLED,
        now.minus(Duration.ofDays(2)),
        now.minus(Duration.ofHours(4)),
        "auction_4_img_1.jpg",
        "auction_4_img_2.jpg",
        "auction_4_img_3.jpg"
      )
    );

    items.add(
      createAuction(
        105,
        "Ended Vintage Camera With Zero Bids",
        "A retired camera listing used to test ended-state handling in the UI and relist actions.",
        "Electronics",
        55000,
        55000,
        null,
        "seller-ellen",
        "ENDED",
        now.minus(Duration.ofDays(3)),
        now.minus(Duration.ofHours(12)),
        "auction_5_img_1.jpg",
        "auction_5_img_2.jpg",
        "auction_5_img_3.jpg"
      )
    );

    items.add(
      createAuction(
        106,
        "Oversized Bookshelf Package",
        "Bookshelf listing with a normal amount of text but an intentionally large bid count to exercise currency labels.",
        "Furniture",
        2500000,
        3125000,
        "reader-one",
        "seller-alice",
        Constants.STATUS_ACTIVE,
        now.minus(Duration.ofHours(8)),
        now.plus(Duration.ofDays(2)),
        "auction_6_img_1.jpg",
        "auction_6_img_2.jpg",
        "auction_6_img_3.jpg"
      )
    );

    items.add(
      createAuction(
        107,
        "Cancelled Vinyl Collection",
        "A cancelled batch of classic records with no bids and no images.",
        "Music",
        10000,
        10000,
        null,
        "seller-bob",
        Constants.STATUS_CANCELLED,
        now.minus(Duration.ofDays(1)),
        now.minus(Duration.ofHours(6)),
        "auction_1_img_1.jpg",
        "auction_1_img_2.jpg",
        "auction_1_img_3.jpg"
      )
    );

    items.add(
      createAuction(
        108,
        "Retro Game Console Bundle With Extremely, Almost Comically, Needlessly Long Name For Wrapping Validation Purposes",
        "Bundle contains several cartridges and a controller. This description is also much longer than a normal one to force wrapping.",
        "Electronics",
        78000,
        84500,
        "gamer-zed",
        "seller-charlie",
        Constants.STATUS_ACTIVE,
        now.minus(Duration.ofHours(6)),
        now.plus(Duration.ofHours(6)),
        "auction_2_img_1.jpg",
        "auction_2_img_2.jpg",
        "auction_2_img_3.jpg"
      )
    );

    items.add(
      createAuction(
        109,
        "Auction With Empty Images and No Bids",
        "A plain fallback item that intentionally has all image fields empty.",
        "Home",
        2000,
        2000,
        null,
        "seller-iris",
        Constants.STATUS_ACTIVE,
        now.minus(Duration.ofHours(3)),
        now.plus(Duration.ofHours(3)),
        "auction_3_img_1.jpg",
        "auction_3_img_2.jpg",
        "auction_3_img_3.jpg"
      )
    );

    items.add(
      createAuction(
        110,
        "Professional Camera Kit",
        "Camera body, two lenses, bag, and the kind of mid-length description that looks normal but still needs stable spacing.",
        "Electronics",
        9900000,
        10150000,
        "studio-pro",
        "seller-june",
        Constants.STATUS_EXPIRED,
        now.minus(Duration.ofDays(5)),
        now.minus(Duration.ofHours(10)),
        "auction_4_img_1.jpg",
        "auction_4_img_2.jpg",
        "auction_4_img_3.jpg"
      )
    );

    items.add(
      createAuction(
        111,
        "Custom Desk Lamp with Warm Brass Finish",
        "Small item, ordinary description, no tricks.",
        "Home",
        8000,
        0,
        null,
        "seller-luke",
        Constants.STATUS_ACTIVE,
        now.minus(Duration.ofHours(1)),
        now.plus(Duration.ofHours(18)),
        "auction_5_img_1.jpg",
        "auction_5_img_2.jpg",
        "auction_5_img_3.jpg"
      )
    );

    items.add(
      createAuction(
        112,
        "CANCELLED Cabinet Prototype",
        "This cancelled cabinet listing is another zero-bid fallback to ensure the empty-state UI is never the only path tested.",
        "Furniture",
        120000,
        120000,
        null,
        "seller-mia",
        Constants.STATUS_CANCELLED,
        now.minus(Duration.ofDays(4)),
        now.minus(Duration.ofHours(2)),
        "auction_6_img_1.jpg",
        "auction_6_img_2.jpg",
        "auction_6_img_3.jpg"
      )
    );

    items.add(
      createAuction(
        113,
        "Luxury Smartwatch with Massive Bid Amount",
        "Another high-value item to verify that current bid labels do not overflow or lose separators.",
        "Electronics",
        140000000,
        168000000,
        "watch-queen",
        "seller-nora",
        Constants.STATUS_ACTIVE,
        now.minus(Duration.ofHours(9)),
        now.plus(Duration.ofDays(3)),
        "c0a465b1-fc59-4765-b63f-b5d7dcd961c2_1.jpg",
        "auction_1_img_1.jpg",
        "auction_2_img_1.jpg"
      )
    );

    items.add(
      createAuction(
        114,
        "Archive Box With No Bids",
        "Another zero-bid case for the detail and list views.",
        "Office",
        1500,
        1500,
        null,
        "seller-omar",
        "ENDED",
        now.minus(Duration.ofDays(2)),
        now.minus(Duration.ofHours(1)),
        "auction_3_img_1.jpg",
        "auction_3_img_2.jpg",
        "auction_3_img_3.jpg"
      )
    );

    items.add(
      createAuction(
        115,
        "Statement Sculpture for Modern Interiors",
        "A final mock item with a long enough description to prove text wrapping survives in the gallery and on the detail page.",
        "Art",
        560000,
        560000,
        null,
        "seller-pia",
        Constants.STATUS_ACTIVE,
        now.minus(Duration.ofHours(12)),
        now.plus(Duration.ofDays(1)),
        "auction_4_img_1.jpg",
        "auction_4_img_2.jpg",
        "auction_4_img_3.jpg"
      )
    );

    return items;
  }

  private static AuctionItem createAuction(
    int id,
    String title,
    String description,
    String category,
    long startingPriceCents,
    long currentBidCents,
    String highestBidder,
    String seller,
    String status,
    Instant startTime,
    Instant endTime,
    String img1,
    String img2,
    String img3
  ) {
    return createAuction(
      id,
      title,
      description,
      category,
      startingPriceCents,
      currentBidCents,
      highestBidder,
      seller,
      status,
      startTime,
      endTime,
      null,
      img1,
      img2,
      img3
    );
  }

  private static AuctionItem createAuction(
    int id,
    String title,
    String description,
    String category,
    long startingPriceCents,
    long currentBidCents,
    String highestBidder,
    String seller,
    String status,
    Instant startTime,
    Instant endTime,
    String capEndTime,
    String img1,
    String img2,
    String img3
  ) {
    AuctionItem item = new AuctionItem();
    item.setId(id);
    item.setTitle(title);
    item.setDescription(description);
    item.setCategory(category);
    item.setStartingPriceCents(startingPriceCents);
    item.setCurrentBidCents(currentBidCents);
    item.setHighestBidderUsername(highestBidder);
    item.setSellerUsername(seller);
    item.setStartTime(startTime.toString());
    item.setEndTime(endTime.toString());
    item.setCapEndTime(
      capEndTime == null
        ? endTime
            .plus(Duration.ofMinutes(Constants.SNIPE_CAP_DEFAULT_MINUTES))
            .toString()
        : capEndTime
    );
    item.setStatus(status);
    item.setImg1(img1);
    item.setImg2(img2);
    item.setImg3(img3);
    return item;
  }
}
