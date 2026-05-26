# 🧪 RTDAS Testing Guide with Seeded Data

This guide walks you through setting up test data and validating all RTDAS features, especially for bella-247's bidder experience.

---

## Setup: Create Test Data & Images

### Step 1: Run DemoSeeder

Populates the database with 7 test users, 6 auctions, and 30+ bids:

```bash
mvn exec:java -Dexec.mainClass=com.auction.server.core.DemoSeeder
```

**Output:**

```
✓ 7 users created (3 sellers, 4 bidders)
✓ 6 auctions created
  - 2 short auctions (5 & 3 min) for Reaper testing
  - 2 medium auctions (4 & 12 hours) for UI testing
  - 2 long auctions (24 & 48 hours) for full-day testing
✓ 30+ bids placed across auctions
```

### Step 2: Generate Test Images

Creates colorful placeholder images for each seeded auction:

```bash
mvn exec:java -Dexec.mainClass=com.auction.server.core.SeedTestImages
```

**Output:**

```
📸 Walkman (ID: 1)
  ✓ Full: resources/images/auction_1_img_1.jpg
  ✓ Thumb: resources/thumbs/auction_1_img_1_thumb.jpg
  [... 5 more auctions, 3 images each ...]
✓ All images generated!
📁 Images: resources/images
📁 Thumbnails: resources/thumbs
```

### Step 3: Start Server

```bash
mvn exec:java
```

---

## Test Accounts

| User             | Password  | Role   | Purpose                   |
| ---------------- | --------- | ------ | ------------------------- |
| `admin`          | `admin`   | Admin  | System access, audit logs |
| `seller-alice`   | `pass123` | Seller | Create/manage auctions    |
| `seller-bob`     | `pass123` | Seller | Create/manage auctions    |
| `seller-charlie` | `pass123` | Seller | Create/manage auctions    |
| **`bella-247`**  | `pass123` | Bidder | **Main test user**        |
| `bidder-dan`     | `pass123` | Bidder | Competitive bidding       |
| `bidder-eve`     | `pass123` | Bidder | Competitive bidding       |
| `bidder-frank`   | `pass123` | Bidder | Competitive bidding       |

---

## 🎯 Test Scenarios for bella-247

### 1. **Gallery Browsing & Thumbnail Display**

✅ **What to test:** Image loading, thumbnail rendering, category filtering

1. Login as **bella-247**
2. Navigate to **Gallery**
3. Verify:
    - [ ] Thumbnails display for all 6 auctions
    - [ ] Thumbnails are ~100x100px (not blurry)
    - [ ] Category filter works (Electronics, Furniture, Art)
    - [ ] Sort by price/time works
    - [ ] Clicking thumbnail opens detail view

**Expected:** All 6 colored placeholder images visible with distinct emojis (📱, 🪑, 🎨, 📖, etc.)

---

### 2. **Auction Detail & Real-Time Bidding**

✅ **What to test:** Detail page layout, bid placement, real-time updates, optimistic locking

1. **Click Auction #1: "Vintage Sony Walkman"** (24-hour auction)
    - [ ] Full images load (400x400px)
    - [ ] Title, description, category, price visible
    - [ ] Current bid shows: **$16.00** (pre-seeded)
    - [ ] Highest bidder shows: **bella-247**

2. **Place a bid** (must be ≥ $16.80 = $16.00 × 1.05)
    - [ ] Bid slider/input accepts amount
    - [ ] Spinner shows while submitting
    - [ ] Success toast appears
    - [ ] Current bid updates immediately (optimistic)
    - [ ] Timestamp updates in bid history

3. **Try invalid bids:**
    - [ ] Bid < $16.80 → error "must be at least..."
    - [ ] Bid = $0 → error "must be positive"
    - [ ] Bid = NaN → error
    - [ ] Place same bid twice → error "you are already highest bidder"

4. **Try Snipe Protection:**
    - Click **Auction #5: "Harry Potter"** (expires in 3 minutes)
    - [ ] Place bid within 30 seconds of end time
    - [ ] See "Timer Extended" toast
    - [ ] End time moves +30 seconds further (max = cap_end_time + 10 min)
    - [ ] Place another bid → Timer extends again (up to cap)

---

### 3. **Bidder Activity View (My Activity)**

✅ **What to test:** Bid history, won auctions, outbid notifications

1. Navigate to **My Activity** (or Bidder Dashboard)
2. Click tab: **My Bids**
    - [ ] Shows 3+ bids from bella-247
    - [ ] Each bid shows: auction title, amount, timestamp, status
    - [ ] Bids are sorted newest first
3. Click tab: **Won Auctions**
    - [ ] Shows auctions where bella-247 is highest bidder AND status=SOLD
    - [ ] (None yet - seller hasn't ended auctions)
4. Click tab: **Outbid**
    - [ ] Shows auctions where bella-247 bid but was outbid
    - [ ] Shows winning bidder & winning amount

---

### 4. **Seller Dashboard (Login as seller-alice)**

✅ **What to test:** Seller auctions, admin features, CSV export

1. Login as **seller-alice**
2. Navigate to **My Auctions**
    - [ ] Shows: Walkman, Teak Chair, Bookshelf (3 auctions)
    - [ ] Status column shows: ACTIVE, ACTIVE, ACTIVE
    - [ ] Can view each auction's bid history
3. **Try Cancel:**
    - [ ] Click Cancel on an auction with 0 bids → should succeed
    - [ ] Status changes to CANCELLED
4. **Try Export to CSV:**
    - [ ] Click "Export Auctions"
    - [ ] Download CSV file
    - [ ] Open file, verify:
        - [ ] Header row: id, title, category, starting_price, current_bid, highest_bidder, status, ...
        - [ ] Walkman row: $15.00, $16.00, bella-247, ACTIVE
        - [ ] CSV escaping works (titles with commas/quotes)

---

### 5. **Reaper & Auction Expiration**

✅ **What to test:** Automatic auction closure, state transitions

1. **Track Auction #2 (Atari 2600)** - 5 minute timer
    - [ ] Current status: ACTIVE
    - [ ] Watch countdown timer
    - [ ] At ~4:50 left, see status change to EXPIRED or SOLD
    - [ ] No manual refresh needed (polling works)

2. **Track Auction #5 (Harry Potter)** - 3 minute timer + snipe bids
    - [ ] Bid within 30 sec of end → timer extends
    - [ ] Eventually timer expires
    - [ ] Status becomes SOLD (has bids) or EXPIRED (no bids)

3. **Login as seller-bob** (owns Atari & Harry Potter)
    - [ ] Dashboard shows both auctions now as SOLD/EXPIRED
    - [ ] Can relist an EXPIRED auction with new end time

---

### 6. **Image Upload & Thumbnail Generation**

✅ **What to test:** Server-side image upload, JPG re-encoding, thumbnail generation

1. Login as **seller-charlie**
2. Click **Create New Auction**
3. Fill form:
    - Title: "Test Item"
    - Category: Electronics
    - Starting Price: $10.00
    - Description: "Test description"
    - End Time: 24 hours from now
4. **Upload 3 images:**
    - Image 1: A PNG file → verify converted to JPG
    - Image 2: A PNG file → verify converted to JPG
    - Image 3: A PNG file → verify converted to JPG
5. Click **Create**
    - [ ] Success toast
    - [ ] New auction appears in gallery
    - [ ] Thumbnails display immediately
    - [ ] Full images load in detail view

---

### 7. **Button Functionality Audit**

✅ **What to test:** Verify all buttons do what they claim

| Button               | Location         | Expected Action                            | Status |
| -------------------- | ---------------- | ------------------------------------------ | ------ |
| **Login**            | Login screen     | Submit credentials, show errors if invalid | ✓      |
| **Register**         | Login screen     | Create new account, show duplicate error   | ✓      |
| **Logout**           | Top nav          | Clear token, redirect to login             | ✓      |
| **Browse Gallery**   | Nav              | Show all active auctions                   | ✓      |
| **Place Bid**        | Auction detail   | Submit bid with validation                 | ✓      |
| **View My Activity** | Nav/Bidder       | Show bids/won/outbid tabs                  | ✓      |
| **My Auctions**      | Nav/Seller       | Show seller's auctions                     | ✓      |
| **Create Auction**   | Seller dashboard | Open form, accept images                   | ✓      |
| **Cancel Auction**   | Seller dashboard | Cancel with 0 bids, error if has bids      | ✓      |
| **Relist Auction**   | Seller dashboard | Relist expired auction with new date       | ✓      |
| **Export to CSV**    | Seller dashboard | Download CSV file                          | ✓      |
| **Refresh**          | Any page         | Reload data from server                    | ✓      |

---

## 📊 Expected State After Seeding

### Users (7)

- 3 sellers (alice, bob, charlie)
- 4 bidders (bella-247, dan, eve, frank)

### Auctions (6) with Status ACTIVE

| ID  | Title        | Seller  | Starting Price | Current Bid | Highest Bidder | Ends In   |
| --- | ------------ | ------- | -------------- | ----------- | -------------- | --------- |
| 1   | Walkman      | alice   | $15.00         | $16.00      | bella-247      | 24 hours  |
| 2   | Atari 2600   | bob     | $50.00         | $0.00       | (none)         | 5 minutes |
| 3   | Teak Chair   | alice   | $80.00         | $82.00      | frank          | 4 hours   |
| 4   | Oil Painting | charlie | $150.00        | $155.00     | eve            | 48 hours  |
| 5   | Harry Potter | bob     | $200.00        | $205.00     | bella-247      | 3 minutes |
| 6   | Bookshelf    | alice   | $20.00         | $20.30      | bella-247      | 12 hours  |

### Bids (30+)

- bella-247: 9 bids across 4 auctions (leading bidder on 3)
- bidder-dan: 4 bids
- bidder-eve: 4 bids
- bidder-frank: 3 bids

### Images

- 6 auctions × 3 images = 18 full-size images
- 18 thumbnail images (auto-generated)
- Stored in: `resources/images/` and `resources/thumbs/`

---

## ⚠️ Known Limitations / Things to Check

1. **Images display as placeholders** — generated test images are not photorealistic
2. **3 & 5-minute auctions expire quickly** — good for Reaper testing, plan accordingly
3. **Snipe protection has a 10-minute hard cap** — extend beyond that and bid fails
4. **Optimistic locking** — if two clients bid simultaneously, one gets StaleDataException
5. **No persistent polling** — close browser tab, polling stops (expected for demo)
6. **Rate limiting** — login floods are throttled to 5 attempts/min

---

## 🔍 Debugging Commands

### Check Database State

```bash
sqlite3 data/rtdas.db
sqlite> SELECT id, title, status, current_bid_cents, highest_bidder_username FROM auction_items;
sqlite> SELECT * FROM users;
sqlite> SELECT bidder_username, amount_cents, timestamp FROM bids ORDER BY timestamp DESC LIMIT 10;
```

### Check Server Logs

```bash
tail -f logs/logs.txt
```

### Clear & Restart

```bash
rm data/rtdas.db
rm -rf resources/images/* resources/thumbs/*
mvn exec:java -Dexec.mainClass=com.auction.server.core.DemoSeeder
mvn exec:java -Dexec.mainClass=com.auction.server.core.SeedTestImages
mvn exec:java  # start server
```

---

## ✅ Verification Checklist

After running all tests, verify:

- [x] All thumbnails load (no broken images)
- [x] Bidding works with proper validation
- [x] Real-time updates work (poll refreshes every 2s)
- [x] Snipe protection extends end time
- [x] Short auctions expire and change status
- [x] Activity view shows correct bids/wins
- [x] Seller dashboard shows auctions
- [x] CSV export has correct format
- [x] Button clicks produce expected results
- [x] Error messages are clear & helpful
- [x] Images upload, convert, & generate thumbnails
- [x] Outbid notifications work (eventually)

---

**Questions?** Check `docs/` for architecture, design decisions, and implementation details.
