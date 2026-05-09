# PRD: Real-Time Distributed Auction System (RTDAS)

> **Status:** `approved`
> **Created:** 2026-05-06
> **Last Updated:** 2026-05-06
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
30. As a **Bidder**, I want to see the updated end time immediately after a snipe extension, so that I know I have more time to respond.

### Auction Creation & Management (Seller)

31. As a **Seller**, I want to create a new auction with a title, description, category, starting price, end time, and up to 3 images (max 2MB each), so that I can list items for sale.
32. As a **Seller**, I want the client to validate image file sizes before upload and reject files over 2MB with a clear message, so that I don't waste time uploading oversized files.
33. As a **Seller**, I want the system to auto-generate a 40×40 blurred thumbnail from my first image, so that gallery loading is fast.
34. As a **Seller**, I want to cancel an auction that has zero bids, so that I can remove items I no longer want to sell.
35. As a **Seller**, I want to be prevented from cancelling an auction that already has bids, so that bidders are protected.
36. As a **Seller**, I want to see a dashboard of all my auctions grouped by status (active, sold, expired, cancelled), so that I can track my listings.
37. As a **Seller**, I want to see the full bid history and final sale price for my sold auctions, so that I have a complete sales record.
38. As a **Seller**, I want to relist an expired auction that received no bids, so that I can try selling the item again.
39. As a **Seller**, I want to bid on other sellers' auctions, so that I can participate as a buyer too.
40. As a **Seller**, I want to export my auctions to a CSV file via a save dialog, so that I have an offline record of my sales.

### Administration

41. As an **Admin**, I want to back up the SQLite database, so that data is safe.
42. As an **Admin**, I want to view system audit logs, so that I can monitor activity.
43. As an **Admin**, I want to see all users in the system, so that I can manage accounts.

### Auction Lifecycle & State Machine

44. As the **System**, I want auctions to follow a strict state machine (ACTIVE → SOLD | EXPIRED → optionally relisted, or ACTIVE → CANCELLED), so that state transitions are predictable.
45. As the **System**, I want an "Auction Reaper" background thread to scan every 1 second for auctions whose end time has passed and automatically transition them to SOLD (if bids exist) or EXPIRED (if no bids), so that auction closure is automatic and requires no user action.
46. As the **System**, I want to recover from a server crash by expiring all overdue ACTIVE auctions on startup, so that stale auctions don't persist.

### Data Export & Audit

47. As a **Seller**, I want to download a CSV file containing: AuctionID, Title, Category, StartingPrice, FinalPrice, Winner, Status, StartTime, EndTime — so that I have a complete offline record.
48. As the **System**, I want all significant actions (logins, bids, auction creation, cancellation, status transitions) logged to an append-only audit log file, so that there is a tamper-resistant activity record.

### Robustness & Edge Cases

49. As a **Bidder**, I want to see a clear error alert if the server is unreachable, so that I know the connection is down.
50. As a **Bidder**, I want the polling thread to stop automatically when I close the auction detail view, so that resources are cleaned up.
51. As the **System**, I want to return a placeholder image if a file is missing on disk, so that the client never crashes due to missing assets.
52. As the **System**, I want all RMI calls on the client to run in background `Task` threads, so that the JavaFX UI never freezes.
53. As the **System**, I want the server to also validate uploaded images are ≤2MB, so that a malicious or broken client can't bypass client-side checks.

---

## Implementation Decisions

### 1. Build System — Single-Module Maven (Option A)

The project uses a **single Maven module** with package-based separation (`com.auction.shared`, `com.auction.server`, `com.auction.client`). This avoids multi-module overhead while maintaining clean architectural boundaries.

- One root `pom.xml` declares all dependencies (JavaFX 17, SQLite-JDBC) and plugins (javafx-maven-plugin, exec-maven-plugin, maven-shade-plugin).
- Server runs via `mvn exec:java -Dexec.mainClass="com.auction.server.core.ServerLauncher"`.
- Client runs via `mvn javafx:run`.
- Java version: **17** (LTS, required by JavaFX 17+).

### 2. Networking — Java RMI + UDP Discovery

**RMI (primary communication):**
- No RMI callbacks. The client polls the server every 2–3 seconds using `ScheduledExecutorService`.
- This avoids the complexity of RMI callback registration, firewall issues, and client-side RMI export.
- The RMI remote interface (`IAuctionService`) is the sole contract between client and server.

**UDP Broadcast (server discovery):**
- The server broadcasts a small UDP packet every 2 seconds on port **9999** containing `RTDAS|<rmiPort>|<serverName>`.
- Clients listen for these broadcasts on the Connect screen and display discovered servers in a `ListView`.
- Manual IP entry is always available as a fallback.
- This demonstrates network programming beyond RMI (`DatagramSocket`, `DatagramPacket`).

### 3. RMI Interface Contract

The interface will be updated from the current skeleton to include:

- `placeBid(int auctionId, String bidderUsername, double amount, double clientExpectedPrice)` — adds `clientExpectedPrice` for stale-data detection.
- `byte[] getThumbnail(int auctionId, int imageIndex)` — LQIP thumbnail retrieval (index 1–3).
- `byte[] getFullImage(int auctionId, int imageIndex)` — full image retrieval.
- `void cancelAuction(int auctionId, String sellerUsername)` — auction cancellation.
- `void createUser(String username, String password, String role)` — admin user creation.
- `List<User> getAllUsers()` — admin user listing.
- `byte[] backupDatabase()` — admin DB backup.
- `List<String> getAuditLogs(int lastNLines)` — admin log viewing.
- **Remove** `importAuctionsFromCSV` (out of scope per final blueprint).

### 4. Database — SQLite via JDBC

- Single file: `data/auction.db`.
- Three tables: `users`, `auction_items`, `bids`.
- `auction_items` uses a `status` TEXT column with CHECK constraint (`ACTIVE`, `SOLD`, `EXPIRED`, `CANCELLED`) instead of a boolean `active` flag. The `AuctionItem` model must be updated to use `String status` and add `category`, `img1`, `img2`, `img3` fields.
- **All timestamps stored and transported as ISO-8601 String** (`"2026-05-06T14:30:00"`) in both the database and RMI models. Conversion to `LocalDateTime` happens only in display logic on the client. This eliminates serialization ambiguity.
- `DatabaseHandler` owns the single `Connection` and provides all CRUD methods. Schema is auto-created on first run via `CREATE TABLE IF NOT EXISTS`.
- Default admin account (`admin`/`admin`) is auto-seeded if the users table is empty.

### 5. Concurrency Model

**Server-side:**
- `placeBid()` uses per-auction locking via `ConcurrentHashMap<Integer, ReentrantLock>` to prevent race conditions without blocking unrelated auctions.
- Validation inside the lock: `bidder != seller`, `amount >= currentBid * 1.05`, `clientExpectedPrice == currentBid`, `status == ACTIVE`, `now < endTime`.
- Snipe protection: if `endTime - now < 30s`, extend `endTime += 30s`.
- **Auction Reaper** thread: a `ScheduledExecutorService` that fires every 1 second, queries for `status = 'ACTIVE' AND end_time < now()`, and transitions matching auctions to SOLD (if bids exist) or EXPIRED (if none). This is the mechanism that automatically closes auctions — no user action required.

**Client-side:**
- All RMI calls wrapped in `javafx.concurrent.Task` to keep the UI responsive.
- Polling via `ScheduledExecutorService` every 2 seconds; results dispatched via `Platform.runLater()`.
- Polling stops when the detail view is closed (cleanup in controller's `shutdown()` method).

**Capacity:** 4 simultaneous clients polling every 2 seconds = ~8 RMI calls/second. Per-auction locking means concurrent bids on different items never block each other. Zero strain on the server.

### 6. Image Handling — LQIP Pattern

**Upload (server-side):**
- Client validates file size ≤2MB before sending. Server also validates on receipt.
- Client sends `byte[]` + image index (1–3) during auction creation.
- Server saves full image as `resources/images/<auctionId>_<index>.jpg`.
- For index 1, server generates a 40×40 blurred thumbnail using Java's built-in `java.awt.image.BufferedImage` and `javax.imageio` (no external dependency) and saves to `resources/thumbs/<auctionId>_1.jpg`.

**Download (client-side):**
- Gallery: loads only thumbnails (tiny, fast).
- Detail view: immediately shows thumbnail, starts background `Task` to load full image 1. Images 2 and 3 loaded on demand when the user clicks their thumbnail preview.
- If image file is missing on disk, server returns a built-in placeholder byte array.

### 7. Security

- Passwords hashed with `SHA-256` via `java.security.MessageDigest` before storage.
- No salt (acceptable for university demo scope).
- Admin creates all accounts; no self-registration endpoint.

### 8. Auction State Machine

```
         ┌─────────────┐
         │   ACTIVE     │
         └──────┬───────┘
                │
        ┌───────┼────────┐
        │       │        │
   (has bids) (no bids) (seller cancels,
        │       │        0 bids)
        ▼       ▼        ▼
     ┌──────┐ ┌────────┐ ┌───────────┐
     │ SOLD │ │EXPIRED │ │ CANCELLED │
     └──────┘ └───┬────┘ └───────────┘
                  │
              (relist)
                  │
                  ▼
              ┌────────┐
              │ ACTIVE │
              └────────┘
```

### 9. CSV Export Specification

The CSV export is available to Sellers from the Seller Dashboard. It contains all of their auctions (any status).

**Columns:**

| Column | Source | Notes |
|--------|--------|-------|
| AuctionID | `auction_items.id` | |
| Title | `auction_items.title` | |
| Category | `auction_items.category` | |
| StartingPrice | `auction_items.starting_price` | |
| FinalPrice | `auction_items.current_bid` | Same as starting price if no bids |
| Winner | `auction_items.highest_bidder_username` | Empty string if no bids |
| Status | `auction_items.status` | ACTIVE, SOLD, EXPIRED, CANCELLED |
| StartTime | `auction_items.start_time` | ISO-8601 |
| EndTime | `auction_items.end_time` | ISO-8601 |

The CSV is generated server-side (demonstrating server File I/O) and returned as `byte[]` over RMI. The client saves it via a JavaFX `FileChooser` save dialog (demonstrating client File I/O). Dual-sided File I/O usage.

### 10. Audit Log Format

Stored at `logs/audit.log`. Append-only. One line per event.

```
[2026-05-06T14:30:00] [INFO] admin: Created user 'seller1' with role SELLER
[2026-05-06T14:31:15] [INFO] seller1: Created auction #5 'Vintage Guitar'
[2026-05-06T14:32:00] [INFO] bidder1: Placed bid $150.00 on auction #5
[2026-05-06T14:32:28] [INFO] REAPER: Auction #3 expired (no bids)
[2026-05-06T14:32:28] [INFO] REAPER: Auction #4 sold to bidder2 for $320.00
[2026-05-06T14:33:00] [WARN] bidder1: Bid rejected on auction #5 (stale price)
```

Format: `[ISO-timestamp] [LEVEL] actor: description`

### 11. User Model Hierarchy (OOP)

```
User (abstract, Serializable)
├── Admin
├── Seller
└── Bidder
```

- `User` stores `username`, `passwordHash`, `roleType`.
- Subclasses demonstrate inheritance. Role-based behaviour is driven by the `roleType` string at the controller level.
- A Seller inherits all Bidder capabilities (can browse and bid on others' items) plus has access to the Seller Dashboard for managing their own listings and sales history.

### 12. JavaFX Views & Application Flow

**Flow:**
```
App Start → Connect Screen → Login Screen → Role-appropriate views
```

**Views:**

| View | FXML | Controller | Roles | Purpose |
|------|------|-----------|-------|---------|
| Connect | `connect.fxml` | `ConnectController` | All (pre-login) | Auto-discover or manually enter server IP |
| Login | `login.fxml` | `LoginController` | All | Authenticate with username/password |
| Admin Panel | `admin_panel.fxml` | `AdminPanelController` | Admin | Create users, backup DB, view logs |
| Auction Gallery | `gallery.fxml` | `GalleryController` | Seller, Bidder | Browse/filter/sort active auctions |
| Auction Detail | `auction_detail.fxml` | `AuctionDetailController` | Seller, Bidder | View item, bid, see history, countdown |
| Seller Dashboard | `seller_dashboard.fxml` | `SellerDashboardController` | Seller | Own auctions by status, create/cancel/relist, export CSV |

- Modern styling via CSS (dark theme).
- `TableView` for bid history and auction lists, `ImageView` for product images, `ComboBox` for categories.
- Errors shown as `Alert` dialogs.

### 13. Categories

Fixed set, implemented as a Java enum:
- `ELECTRONICS`
- `FURNITURE`
- `ART`
- `OTHER`

---

## Testing Decisions

### What makes a good test

Tests verify **external behaviour through public interfaces**, not implementation details. A test should:
- Set up state (seed the database or create objects).
- Invoke a public method.
- Assert on the observable outcome (return value, database state change, exception thrown).
- Never assert on private field values or internal method call counts.

### Modules to test

| Module | Priority | What to test |
|--------|----------|-------------|
| **DatabaseHandler** | High | Schema auto-creation on fresh DB. User CRUD (insert, lookup, password hash verification). Auction CRUD (create, retrieve by ID, retrieve active, update status). Bid insertion and history retrieval ordered by timestamp. Edge cases: duplicate username, bid on non-existent auction. |
| **AuctionServiceImpl** | High | Successful bid placement (amount ≥ currentBid × 1.05). Rejection: below minimum increment. Rejection: bidder is seller. Rejection: already highest bidder. Rejection: stale price mismatch. Rejection: auction not ACTIVE or past end time. Snipe protection: bid in last 30s extends end time. Concurrent bid placement: two threads — only one wins. |
| **AuctionReaper** | Medium | Auction with bids past end time → SOLD. Auction without bids past end time → EXPIRED. Auction with future end time → unchanged. |
| **FileHandler** | Medium | CSV export generates correct columns and data. Image file saving and retrieval. Placeholder returned when image file missing. Audit log append-only writing. |

**How:** All database-backed tests use an in-memory SQLite database (`jdbc:sqlite::memory:`) for fast, isolated execution.

### Modules NOT tested (and why)

- **Controllers, ViewLoader, ClientLauncher:** Thin UI wiring. Testing requires TestFX, adding disproportionate complexity for a university project.
- **RmiClientProvider, ServerLauncher, ConnectController:** Integration-only; tested manually during demo.

### Prior art

No existing tests in the codebase. Tests will be created from scratch using JUnit 5.

---

## Out of Scope

1. **Internet deployment** — Local Wi-Fi only. No cloud, NAT traversal, or TLS.
2. **RMI callbacks / server push** — Notifications use client polling.
3. **User self-registration** — Admin-only account creation.
4. **Reserve prices** — Starting price is the minimum.
5. **Payment processing** — No real money.
6. **Multi-module Maven** — Single-module (Option A).
7. **Salted password hashing** — SHA-256 without salt is acceptable for demo.
8. **Automated CI/CD** — Manual build and run.
9. **Internationalisation (i18n)** — English only.
10. **Accessibility (a11y)** — Not a grading criterion.
11. **CSV import** — Only CSV export is implemented.
12. **Admin approval of client connections** — Server is public; any client can connect.

---

## Further Notes

### Demo Day Setup (1 server + 4 clients)

1. **Your PC (Server):** Run `mvn exec:java -Dexec.mainClass="com.auction.server.core.ServerLauncher"`. The server starts broadcasting on UDP port 9999.
2. **Teammates' PCs (Clients):** Run `mvn javafx:run`. The Connect screen auto-discovers the server. Click "Connect", then log in.
3. **Seed data:** The server auto-creates an `admin/admin` account on first run. Use Admin Panel to create seller and bidder accounts for teammates.
4. **Demo script:** Have teammates simultaneously bid on the same auction to showcase real-time concurrency and snipe protection.

### Grading Alignment

| Requirement | Where it appears |
|-------------|-----------------|
| OOP (inheritance, polymorphism, abstraction) | `User` → `Admin`/`Seller`/`Bidder` hierarchy, `Serializable`, `Comparable` |
| Collections | `HashMap` auction cache, `PriorityQueue` bid tracking, `List` results, `Comparator` sorting |
| Multithreading | `AuctionReaper` thread, `ReentrantLock` in `placeBid`, client polling thread, JavaFX `Task`, UDP broadcast thread |
| File I/O | CSV export (server generates, client saves), audit log, image read/write (binary streams) |
| JDBC | `DatabaseHandler` — full SQLite CRUD via `java.sql.*` |
| RMI | `IAuctionService` remote interface, `UnicastRemoteObject`, `LocateRegistry` |
| Networking (bonus) | UDP broadcast/discovery via `DatagramSocket` |
| GUI | JavaFX with FXML, CSS styling, `TableView`, `ImageView`, `ComboBox`, `Alert`, `FileChooser` |

### Risk Mitigations

| Risk | Mitigation |
|------|-----------|
| `LocalDateTime` serialization issues over RMI | All timestamps are ISO-8601 Strings in models; parse to `LocalDateTime` only for display. |
| Image file missing on disk | Server returns a built-in placeholder byte array. Client shows a default "no image" graphic. |
| Server crash mid-auction | Reaper runs on startup and closes all overdue ACTIVE auctions immediately. |
| Wi-Fi instability during demo | Client shows "Connection lost" alert. Polling auto-retries. Manual reconnect available via Connect screen. |
