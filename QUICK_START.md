# 🚀 RTDAS Quick Start - Bella-247 Testing

## TL;DR

```bash
# 1. Seed database & generate test images
./seed-demo-data.bat        # Windows
./seed-demo-data.sh         # Mac/Linux

# 2. Start server
mvn exec:java

# 3. Login (client UI or telnet)
Username: bella-247
Password: password
```

---

## 🎯 What's Been Created

| Entity       | Count               | Details                                   |
| ------------ | ------------------- | ----------------------------------------- |
| **Users**    | 7                   | 3 sellers + 4 bidders                     |
| **Auctions** | 6                   | Various durations: 3-48 hours             |
| **Bids**     | 30+                 | bella-247 is leading bidder on 3 auctions |
| **Images**   | 18 full + 18 thumbs | Colorful test placeholders                |

---

## 👥 Test Accounts for bella-247

| Account        | Password | Role              |
| -------------- | -------- | ----------------- |
| bella-247      | pass123  | **Bidder (Main)** |
| seller-alice   | pass123  | Seller            |
| seller-bob     | pass123  | Seller            |
| seller-charlie | pass123  | Seller            |
| bidder-dan     | pass123  | Bidder            |
| admin          | admin    | Admin             |

---

## 🧪 Quick Test Scenarios (10 min each)

### Scenario 1: Gallery & Thumbnails (2 min)

```
1. Login as bella-247
2. Open Gallery
3. Verify: All 6 thumbnails display with colored backgrounds & emojis
4. Click one → Detail view opens with full images
```

✅ **Pass criteria:** Images load, no broken img icons

---

### Scenario 2: Place a Bid (3 min)

```
1. Click "Vintage Sony Walkman" (24 hour auction)
2. Current bid: $16.00 (bella-247 leading)
3. Enter new bid: $17.00 (>= $16.80 min increment)
4. Click "Place Bid"
5. See: Spinner → Success toast → UI updates
```

✅ **Pass criteria:** Bid accepted, UI reflects new price/bidder instantly

---

### Scenario 3: Snipe Protection (2 min)

```
1. Click "Rare Harry Potter" (expires in 3 min)
2. Place bid within 30 sec of end time
3. See: "Timer Extended" toast appears
4. End time +30s in UI
```

✅ **Pass criteria:** Timer extends when bidding near end

---

### Scenario 4: My Activity (2 min)

```
1. Click "My Activity" in nav
2. Tab: "My Bids" → Shows 9 bella-247 bids
3. Tab: "Outbid" → Shows 1 auction (Frank outbid her on Chair)
4. Tab: "Won" → Empty (auctions haven't ended yet)
```

✅ **Pass criteria:** Correct bids shown across tabs

---

### Scenario 5: Seller Dashboard (3 min)

```
1. Logout, login as seller-alice
2. Open "My Auctions"
3. See: Walkman, Chair, Bookshelf with bid counts
4. Click "Export" → CSV downloads
5. Open CSV, verify format
```

✅ **Pass criteria:** CSV has headers, data, correct escaping

---

### Scenario 6: Watch Reaper (5 min)

```
1. Track "Atari 2600" auction (5 min duration)
2. Watch status countdown
3. At 0:00, status changes ACTIVE → EXPIRED (or SOLD)
4. No manual refresh needed (polling)
```

✅ **Pass criteria:** Status changes automatically after expiry

---

### Scenario 7: Image Upload (3 min)

```
1. Login as seller-bob
2. Create new auction, upload 3 images (PNG/JPG)
3. Submit
4. Check gallery for new auction with thumbnails
```

✅ **Pass criteria:** Images display, thumbnails generated server-side

---

### Scenario 8: Button Functionality Audit (3 min)

Check each button does what it says:

- [ ] Login button → validates credentials
- [ ] Place Bid button → submits bid with validation
- [ ] Cancel Auction button → cancels (only if 0 bids)
- [ ] Export button → downloads CSV
- [ ] Logout button → clears session, redirects to login
- [ ] Refresh button → reloads data

✅ **Pass criteria:** All buttons functional, no dead links

---

## 🔧 Troubleshooting

| Issue                      | Solution                                                                                     |
| -------------------------- | -------------------------------------------------------------------------------------------- |
| "No auctions found"        | Run seeder: `mvn exec:java -Dexec.mainClass=com.auction.server.core.DemoSeeder`              |
| "Images are placeholders"  | Run image generator: `mvn exec:java -Dexec.mainClass=com.auction.server.core.SeedTestImages` |
| "Port 1099 already in use" | Kill existing server or change `rmiregistry` port                                            |
| "Database locked"          | Close client connections, try again                                                          |
| "Thumbnails are blurry"    | They're intentionally small (40x40px); full images are crisp (400x400px)                     |

---

## 📊 What to Watch For

✅ **Good Signs:**

- Thumbnails load instantly from cache
- Bids update in real-time (every 2 sec poll)
- Snipe protection toasts appear
- CSV downloads without errors
- Auctions expire automatically

❌ **Red Flags:**

- Broken image icons (img not found)
- Buttons click but do nothing
- Bid placed twice (duplicate entry)
- Status doesn't update after expiry
- CSV contains invalid escaping

---

## 📚 Full Documentation

For detailed test scenarios, edge cases, and button audit:
👉 **See: [TESTING_GUIDE.md](TESTING_GUIDE.md)**

---

## 💡 Pro Tips

1. **Multiple terminals:** Run server in one, clients in others
2. **Rapid bidding:** Test optimistic locking by bidding fast with 2 clients
3. **Watch the Reaper:** Auction #2 & #5 expire in 5 & 3 min respectively
4. **Seller view:** Login as seller-alice to see dashboard features
5. **Database inspect:** `sqlite3 data/rtdas.db ".tables"` to check state

---

**Questions?** Check the Architecture & Design docs in `docs/`.
