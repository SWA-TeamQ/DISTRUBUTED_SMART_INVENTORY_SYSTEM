# PRD: Real-Time Distributed Auction System (RTDAS)

> **Status:** `approved`  
> **Created:** 2026-05-06  
> **Last Updated:** 2026-05-09  
> **Authors:** SWA-TeamQ  
> **Demo Setup:** 1 server (you) + 4 clients (teammates), same local Wi-Fi

---

## Problem Statement

University students need to demonstrate mastery of advanced Java topics — OOP, collections, multithreading, file I/O, JDBC, and Java RMI — through a single cohesive project. Building isolated toy examples for each topic fails to show how these concepts interact in a real system. Students need a project that naturally demands all of these techniques working together, while remaining demonstrable on two or more laptops connected via local Wi-Fi.

## Solution

A distributed, multi-user **English auction platform** where a headless RMI server manages auctions, persists data in SQLite, and serves one or more JavaFX desktop clients. The system supports three roles (Admin, Seller, Bidder) with real-time bid updates via client polling, image galleries with LQIP (Low-Quality Image Placeholder) loading, automatic server discovery via UDP broadcast, and robust concurrency controls including snipe protection.

**Demo environment:** One PC runs the server (you); four teammate PCs connect as clients, all on the same local Wi-Fi. Clients discover the server automatically via UDP broadcast or connect manually by entering the server's IP. No internet required.

---

## User Stories

### Server Discovery & Connection

1. As a **User**, I want the app to automatically detect available auction servers on my local network, so that I don't have to type an IP address.
2. As a **User**, I want to see a list of discovered servers with their names, so that I can pick the right one.
3. As a **User**, I want to manually enter a server IP and port if auto-discovery doesn't work, so that I always have a fallback.
4. As a **User**, I want to see a clear error message if the server is unreachable when I try to connect, so that I know the problem before reaching the login screen.
5. As the **Server**, I want to broadcast my presence on the local network every 2 seconds via UDP, so that clients can find me without configuration.

### Authentication & User Management

6. As an **Admin**, I want to create new user accounts (with a username, password, and role), so that I control who can access the system.
7. As an **Admin**, I want to assign one of three roles (admin, seller, bidder) when creating a user, so that each person has appropriate permissions.
8. As a **User**, I want to log in with my username and password, so that I can access the system with my assigned role.
9. As a **User**, I want my password stored as a SHA-256 hash, so that raw credentials are never persisted.
10. As a **User**, I want to see a clear error message if my login credentials are incorrect, so that I know what went wrong.
11. As the **System**, I want to reject self-registration attempts, so that only admin-created accounts can access the platform.
12. As the **System**, I want to auto-create a default admin account (`admin`/`admin`) on first run, so that the demo can start immediately without manual database seeding.

### Auction Browsing & Discovery

13. As a **Bidder**, I want to see a gallery of all active auctions with thumbnail images, so that I can quickly browse available items.
14. As a **Bidder**, I want to filter auctions by category (Electronics, Furniture, Art, Other), so that I find items I'm interested in.
15. As a **Bidder**, I want to sort auctions by end time, current bid, or category, so that I can prioritise what to look at.
16. As a **Bidder**, I want to see a small blurred thumbnail (LQIP) load instantly in the gallery, so that the UI feels fast even with slow connections.
17. As a **Bidder**, I want to click on an auction to see its full detail view, so that I can evaluate the item before bidding.
18. As a **Bidder**, I want the full-resolution primary image to load in the background and replace the thumbnail, so that I see a crisp image without blocking the UI.
19. As a **Bidder**, I want to click small thumbnail previews to load additional images (up to 3 total) on demand, so that I only download what I need.

### Bidding

20. As a **Bidder**, I want to place a bid on an active auction, so that I can compete for items.
21. As a **Bidder**, I want the system to enforce a minimum bid increment of 5% above the current price, so that penny-sniping is prevented.
22. As a **Bidder**, I want the system to reject my bid if someone else bid in the meantime (stale price detection), so that I don't accidentally underbid.
23. As a **Bidder**, I want the system to prevent me from bidding on my own auction, so that self-bidding is impossible.
24. As a **Bidder**, I want the system to prevent me from bidding if I'm already the highest bidder, so that I don't waste bids.
25. As a **Bidder**, I want the "Bid" button to be disabled while my bid is being processed, so that I don't accidentally double-click.
26. As a **Bidder**, I want to see the full bid history for any auction (all bidders, amounts, timestamps), so that I can gauge competition.
27. As a **Bidder**, I want the auction detail view to auto-refresh every 2 seconds, so that I see the latest bid without manually refreshing.
28. As a **Bidder**, I want to see a live countdown timer showing time remaining on an auction, so that I know how much time I have.

### Snipe Protection

29. As a **Bidder**, I want the auction timer to extend by 30 seconds if a bid is placed in the last 30 seconds, so that last-second sniping is discouraged.
30. As a **Bidder**, I want the system to cap total extension so auctions don't run forever, so that the demo stays predictable.

### Bidder Activity Dashboard

31. As a **Bidder**, I want to see "My Activity" with tabs for My Bids, Won, and Outbid, so that I can track my bidding history.

### Auction Creation & Management (Seller)

32. As a **Seller**, I want to create a new auction with a title, description, category, starting price, end time, and up to 3 images (max 2MB each), so that I can list items for sale.
33. As a **Seller**, I want the client to validate image file sizes before upload and reject files over 2MB with a clear message, so that I don't waste time uploading oversized files.
34. As a **Seller**, I want the system to auto-generate a 40×40 blurred thumbnail from my first image, so that gallery loading is fast.
35. As a **Seller**, I want to cancel an auction that has zero bids, so that I can remove items I no longer want to sell.
36. As a **Seller**, I want to be prevented from cancelling an auction that already has bids, so that bidders are protected.
37. As a **Seller**, I want to see a dashboard of all my auctions grouped by status (active, sold, expired, cancelled), so that I can track my listings.
38. As a **Seller**, I want to see the full bid history and final sale price for my sold auctions, so that I have a complete sales record.
39. As a **Seller**, I want to relist an expired auction that received no bids as a new auction, so that I can try selling the item again.
40. As a **Seller**, I want to bid on other sellers' auctions, so that I can participate as a buyer too.
41. As a **Seller**, I want to export my auctions to a CSV file via a save dialog, so that I have an offline record of my sales.

### Administration

42. As an **Admin**, I want to back up the SQLite database, so that data is safe.
43. As an **Admin**, I want to view system audit logs, so that I can monitor activity.
44. As an **Admin**, I want to see all users in the system, so that I can manage accounts.

### Auction Lifecycle & State Machine

45. As the **System**, I want auctions to follow a strict state machine (ACTIVE → SOLD | EXPIRED → optionally relisted, or ACTIVE → CANCELLED), so that state transitions are predictable.
46. As the **System**, I want an "Auction Reaper" background thread to scan every 1 second for auctions whose end time has passed and automatically transition them to SOLD (if bids exist) or EXPIRED (if no bids), so that auction closure is automatic and requires no user action.
47. As the **System**, I want to recover from a server crash by expiring all overdue ACTIVE auctions on startup, so that stale auctions don't persist.

### Data Export & Audit

48. As a **Seller**, I want to download a CSV file containing: AuctionID, Title, Category, StartingPrice, FinalPrice, Winner, Status, StartTime, EndTime — so that I have a complete offline record.
49. As the **System**, I want all significant actions (logins, bids, auction creation, cancellation, status transitions) logged to an append-only audit log file, so that there is an activity record.

### Robustness & Edge Cases

50. As a **Bidder**, I want to see a clear error alert if the server is unreachable, so that I know the connection is down.
51. As a **Bidder**, I want the polling thread to stop automatically when I close the auction detail view, so that resources are cleaned up.
52. As the **System**, I want to return a placeholder image if a file is missing on disk, so that the client never crashes due to missing assets.
53. As the **System**, I want all RMI calls on the client to run in background `Task` threads, so that the JavaFX UI never freezes.
54. As the **System**, I want the server to also validate uploaded images are ≤2MB, so that a malicious or broken client can't bypass client-side checks.

---

## Implementation Decisions

### 1. Build System — Single-Module Maven (Option A)

The project uses a **single Maven module** with package-based separation (`com.auction.shared`, `com.auction.server`, `com.auction.client`). This avoids multi-module overhead while maintaining clean architectural boundaries.

### 2. Networking — Java RMI + UDP Discovery

**RMI (primary communication):**
- No RMI callbacks. Clients poll every 2s via `ScheduledExecutorService`.
- Avoids callback registration complexity and firewall issues.
- `IAuctionService` is the sole contract between client and server.

**UDP Broadcast (server discovery):**
- Server broadcasts `RTDAS|v1|<rmiPort>|<serverName>|<serverId>|<rmiHost>` every 2s on port 9999.
- Clients listen and display in `ListView`.
- Manual entry available as fallback.

### 3. RMI Interface Contract

All mutating methods require a session token from `login()`.

| Method | Signature | Notes |
|--------|-----------|-------|
| login | `Session login(String u, String p)` | Returns token |
| logout | `void logout(String token)` | Invalidates session |
| serverTime | `String serverTime()` | UTC ISO-8601, for clock sync |
| placeBid | `void placeBid(int id, String token, long cents, long expected)` | Stale detection |
| getActiveAuctions | `List<AuctionItem> getActiveAuctions(String token)` | Gallery feed |
| getAuctionById | `AuctionItem getAuctionById(int id, String token)` | Detail view |
| getBidHistory | `List<Bid> getBidHistory(int id, String token)` | History table |
| createAuction | `int createAuction(AuctionItem, byte[]1,2,3, String token)` | Returns new ID |
| cancelAuction | `void cancelAuction(int id, String token)` | Zero bids only |
| relistAuction | `int relistAuction(int id, String newEnd, String token)` | Creates new row |
| getMyBids | `List<Bid> getMyBids(String token)` | Bidder activity |
| getMyWonAuctions | `List<AuctionItem> getMyWonAuctions(String token)` | Bidder activity |
| exportAuctionsToCSV | `byte[] exportAuctionsToCSV(String token)` | Seller dashboard |
| createUser | `void createUser(String adminToken, ...)` | Admin only |
| getAllUsers | `List<User> getAllUsers(String adminToken)` | Admin only |
| backupDatabase | `byte[] backupDatabase(String adminToken)` | Via VACUUM INTO |
| getAuditLogs | `List<String> getAuditLogs(String adminToken, int n)` | Admin only |
| getThumbnail | `byte[] getThumbnail(int id, int idx, String token)` | LQIP |
| getFullImage | `byte[] getFullImage(int id, int idx, String token)` | Full-res |

### 4. Database — SQLite via JDBC

- Single file: `data/auction.db.sqlite`.
- Three tables: `users`, `auction_items`, `bids`.
- **All monetary values are INTEGER cents** (see rationale below).
- **All timestamps are ISO-8601 UTC strings with Z suffix.**
- `PRAGMA foreign_keys = ON` enabled per connection.
- Schema auto-created on first run via `CREATE TABLE IF NOT EXISTS`.

### 5. Currency Model — Integer Cents

All prices and bids stored as `INTEGER` cents (not `DOUBLE` dollars).

**Rationale:** Floating-point equality checks are brittle. In a bidding system where `clientExpectedPrice == currentPrice` must work reliably, integer comparison avoids rounding errors entirely.

### 6. Concurrency Model

**Server-side:**
- Per-auction `ReentrantLock` map prevents race conditions.
- `placeBid()` and `AuctionReaper` share the same lock for an auction.
- Transactions wrap bid insert + auction update together.

**Client-side:**
- All RMI calls in `javafx.concurrent.Task`.
- Polling via `ScheduledExecutorService` every 2s.
- Results via `Platform.runLater()`.
- Thread cleanup on view exit.

### 7. Snipe Protection with Cap

- Trigger: `endTime - now < 30s`
- Effect: `endTime = min(endTime + 30s, capEndTime)`
- `capEndTime = originalEndTime + 10 minutes`

### 8. Image Handling — LQIP Pattern

- Server re-encodes all uploads to JPG (no external dependency).
- Center-crop 40×40 thumbnail generated from image 1.
- Missing images return built-in placeholder bytes.
- Client caches images in-memory per session.

### 9. Security

- Passwords: SHA-256 hash (no salt, acceptable for demo).
- Authentication: Session tokens with server-side map.
- Admin-only registration.
- Rate limiting on `login` and `placeBid` per-IP.

### 10. Auction State Machine

```
ACTIVE ──(has bids)──→ SOLD
  │
  ├──(no bids)────────→ EXPIRED ─/relist/→ ACTIVE (new row)
  │
  └──(cancel, 0 bids)─→ CANCELLED
```

### 11. Audit Log Format

Append-only text at `logs/audit.log`.

```
[2026-05-09T14:30:00Z] [INFO] admin: Created user 'seller1' with role SELLER
[2026-05-09T14:31:15Z] [INFO] seller1: Created auction #5 'Vintage Guitar'
[2026-05-09T14:32:00Z] [INFO] bidder1: Placed bid 15000 on auction #5
[2026-05-09T14:32:28Z] [INFO] REAPER: Auction #3 expired (no bids)
[2026-05-09T14:32:28Z] [INFO] REAPER: Auction #4 sold to bidder2 for 32000
[2026-05-09T14:33:00Z] [WARN] bidder1: Bid rejected on auction #5 (stale price)
```

### 12. JavaFX Views & Application Flow

```
App Start → Connect → Login → {Admin Panel | Gallery | Bidder Dashboard | Seller Dashboard}
```

### 13. Categories

Java enum: `ELECTRONICS`, `FURNITURE`, `ART`, `OTHER`.

---

## Testing Decisions

See `docs/database.md#Testing` for full test matrix.

---

## Out of Scope

1. Internet deployment
2. RMI callbacks / server push
3. User self-registration
4. Reserve prices
5. Payment processing
6. Multi-module Maven
7. Salted password hashing
8. CI/CD
9. Internationalisation
10. Accessibility
11. CSV import
12. Admin approval of connections

---

## Demo Day & Grading

See `docs/demo-runbook.md`.

---

## Cross-References

| Topic | Document |
|-------|----------|
| UI Design | `docs/DESIGN.md` |
| Architecture | `docs/architecture.md` |
| Database | `docs/database.md` |
| Networking | `docs/networking.md` |
| Demo Runbook | `docs/demo-runbook.md` |