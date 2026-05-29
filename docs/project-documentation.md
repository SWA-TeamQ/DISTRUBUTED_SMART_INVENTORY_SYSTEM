# Real-Time Distributed Auction System — Complete Project Documentation

> **Team:** Amira, Barok, Betty, Bemigbar  
> **Course:** OOP Project 2  
> **Tech Stack:** Java 17, JavaFX 17, SQLite, Java RMI, UDP

---

Archived documents (moved to reduce noise): see [docs/archive/README.md](docs/archive/README.md)


## Table of Contents

1. [Project Overview](#1-project-overview)
2. [System Architecture](#2-system-architecture)
3. [Folder & Module Breakdown](#3-folder--module-breakdown)
4. [Important Class Documentation](#4-important-class-documentation)
5. [Important Function Documentation](#5-important-function-documentation)
6. [Networking & Communication Flow](#6-networking--communication-flow)
7. [Database & System State](#7-database--system-state)
8. [Concurrency & Multithreading](#8-concurrency--multithreading)
9. [Frontend/UI Flow](#9-frontendui-flow)
10. [Security & Validation](#10-security--validation)
11. [Execution Flow](#11-execution-flow)
12. [Presentation Preparation Notes](#12-presentation-preparation-notes)
13. [Code Quality & Engineering Analysis](#13-code-quality--engineering-analysis)

---

## 1. Project Overview

### What the Project Does

RTDAS is a **distributed, multi-user English auction platform**. An English auction is the familiar ascending-price format: items start at a low price, bidders compete by raising the bid, and when the timer expires the highest bidder wins.

The system runs on a local network (LAN). A headless RMI server manages auctions, stores data in SQLite, and multiple JavaFX desktop clients connect to browse auctions and place bids in real time.

### Main Problem Solved

University students need to demonstrate mastery of advanced Java topics — OOP, collections, multithreading, file I/O, JDBC, and Java RMI — through a single cohesive project. RTDAS bundles all of these into one working system.

### Main Features

| Feature | Description |
|---------|-------------|
| **English Auction Engine** | 5% minimum bid increment, ascending-price format |
| **Real-Time Bidding** | Clients poll server every 1-2 seconds for live updates |
| **Snipe Protection** | Bids in the last 30 seconds extend the auction by 30s (capped at +10 min) |
| **UDP Server Discovery** | Clients auto-discover servers on LAN without configuration |
| **User Roles** | ADMIN (manage users, backup) and USER (browse, bid, create auctions) |
| **Image Gallery with LQIP** | Low-quality image placeholders that resolve to full resolution |
| **Auction Lifecycle** | Background reaper thread transitions ACTIVE → SOLD/EXPIRED every 1s |
| **Crash Recovery** | On restart, the reaper expires auctions that ended while server was down |
| **CSV Export** | Download auction listings as CSV (RFC 4180) |
| **Database Backup** | Admin can trigger SQLite `VACUUM INTO` backup over RMI |

### Technologies Used

| Layer | Technology | Version |
|-------|------------|---------|
| Language | Java | 17 |
| GUI | JavaFX (Controls + FXML) | 17.0.11 |
| Theme | AtlantaFX PrimerDark | 2.0.1 |
| Icons | Ikonli FontAwesome 5 | 12.3.1 |
| Networking | Java RMI + UDP | Built-in |
| Database | SQLite via JDBC | 3.45.1.0 |
| Build | Maven (single module) | — |
| Testing | JUnit Jupiter | 5.10.2 |

### High-Level Workflow

```
User starts client → UDP discovers server → Login screen
    ↓
Gallery (browse auctions, 2s polling)
    ↓
Auction Detail (view item, bid, 1s polling)
    ↓
Place Bid → RMI call → Server validates (5% increment, stale check,
                         self-bid check, snipe protection)
    ↓
Success → UI updates optimistically → Toast notification
```

---

## 2. System Architecture

### Overall Architecture

The system uses a **Deep Module Architecture** on the server side. The key principle: core business logic lives in "deep" manager classes, while the networking layer is a "shallow" adapter.

```
┌────────────────────────┐     RMI      ┌─────────────────────────────┐
│   JavaFX Desktop       │◄────────────►│   RMI Remote Object         │
│   Client (8 screens)   │              │   AuctionServiceImpl        │
│                        │              │   (thin adapter)            │
└────────────────────────┘              └──────────┬──────────────────┘
                                                   │ delegates to
                                                   ▼
┌──────────────────────────────────────────────────────────────────────┐
│  Deep Modules (core business logic)                                  │
│  ┌────────────────┐  ┌──────────────────┐  ┌──────────────────────┐  │
│  │ AuctionManager  │  │ LifecycleManager │  │ ImageStore            │  │
│  │ • placeBid()    │  │ • sweepOverdue() │  │ • stageImages()      │  │
│  │ • createAuction │  │ • activateSched. │  │ • loadFullImage()    │  │
│  │ • cancelAuction │  └──────────────────┘  │ • loadThumbnail()    │  │
│  │ • relistAuction │                        └──────────────────────┘  │
│  │ • startAuction  │                                                 │
│  └────────┬────────┘                                                 │
│           │ uses                                                     │
│           ▼                                                          │
│  ┌────────────────┐  ┌──────────────────┐  ┌──────────────────────┐  │
│  │ LockManager     │  │ TransactionMgr   │  │ AdminManager          │  │
│  │ (per-auction    │  │ (autoCommit/     │  │ • CSV export         │  │
│  │  ReentrantLock) │  │  commit/rollback)│  │ • backup DB          │  │
│  └────────────────┘  └──────────────────┘  │ • audit logs          │  │
│                                             └──────────────────────┘  │
└──────────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│  Repository Layer (JDBC)                                             │
│  ┌──────────────────┐  ┌──────────────────┐  ┌────────────────────┐  │
│  │ UserRepository    │  │ AuctionRepository│  │ BidRepository       │  │
│  │ • findUserByUser  │  │ • findAuctionBy..│  │ • insertBid()      │  │
│  │ • insertUser()    │  │ • insertAuction()│  │ • findBidsByAuction│  │
│  │ • findAllUsers()  │  │ • searchActive.. │  │ • findBidsByBidder │  │
│  └──────────────────┘  └──────────────────┘  └────────────────────┘  │
│                                   │                                  │
│                             ┌─────▼─────┐                           │
│                             │   SQLite   │                           │
│                             │ auction.db │                           │
│                             └───────────┘                           │
└──────────────────────────────────────────────────────────────────────┘
```

### Client-Server Communication

| Protocol | Purpose | Details |
|----------|---------|---------|
| **UDP** | Server discovery | Server broadcasts `RTDAS|MainServer|1099` on port 9999 every 2s. Client listens on same port. |
| **RMI** | All application logic | Java RMI on port 1099. Client looks up `"AuctionService"` in the RMI registry. |

### Main Modules

| Module | Package | Responsibility |
|--------|---------|----------------|
| **shared** | `com.auction.shared` | RMI interface, models, enums, exceptions, constants. Used by both client and server. |
| **server.core** | `com.auction.server.core` | Deep modules: `AuctionManager`, `LifecycleManager`, `ImageStore`, `AdminManager`, `LockManager`, `TransactionManager` |
| **server.service** | `com.auction.server.service` | `AuctionServiceImpl` (RMI adapter), `AuctionReaper` (background sweeper) |
| **server.repository** | `com.auction.server.repository` | `DatabaseManager`, `UserRepository`, `AuctionRepository`, `BidRepository` |
| **client.controllers** | `com.auction.client.controllers` | 8 JavaFX controllers (one per screen) |
| **client.network** | `com.auction.client.network` | `RmiClientProvider`, `UdpDiscoveryClient` |
| **client.service** | `com.auction.client.service` | `PollingService`, `BidHistoryService`, `ThumbnailExecutor` |

### Data Flow (Bidding Example)

```
1. Client: User enters $150, clicks "Place Bid"
2. Client: Optimistic UI update (shows $150 immediately)
3. Client: service.placeBid(auctionId=42, amountCents=15000,
             clientExpectedPriceCents=14200, token="abc...")
       ↓ RMI
4. Server: AuctionServiceImpl.placeBid()
5. Server: validateSession(token) → SessionContext("alice","USER")
6. Server: AuctionManager.placeBid(42, ctx, 15000, 14200)
7. Server: lockManager.lock(42) — acquire per-auction lock
8. Server: txManager.executeWithoutResult(() → {
     validateActive()          — status must be ACTIVE
     validateNotSeller()       — alice ≠ seller
     validateNotCurrentWinner()— alice ≠ highestBidder
     validateFreshness()       — 14200 == currentBidCents?
     validateMinimumBid()      — 15000 ≥ 14200 + 5%?
     validateNotExpired()      — now < endTime?
     applySnipeProtection()    — extend if < 30s left
     bidRepo.insertBid(bid)
     auctionRepo.updateAuctionBid(...)
     auctionRepo.updateAuctionEndTime(...) // if snipe
     AsyncLogger.log(BID, PLACE_BID, ...)
   })
9. Server: lockManager.unlock(42)
       ↓ RMI (void return)
10. Client: on success → Toast "Bid placed!", animate hero image
           on failure → rollback optimistic UI, show Alert
```

### Real-Time Behavior

"Real-time" is achieved via **client-side polling**, not server push:

| Screen | Poll Interval | What is Polled |
|--------|---------------|----------------|
| Gallery | Every 2 seconds | `getAllAuctions()` |
| Auction Detail | Every 1 second | `getAuctionById()` |
| Countdown Timer | Every 1 second | JavaFX `Timeline` (local, with server clock offset) |
| Auto-Launch Watcher | Every 2 seconds | `getAllAuctions()` → detects SCHEDULED → ACTIVE transitions |

---

## 3. Folder & Module Breakdown

### Root Level

| File/Dir | Purpose |
|----------|---------|
| `pom.xml` | Maven build: Java 17, all dependencies and plugins |
| `README.md` | Quick start and docs index |
| `sample_data.sql` | Pre-populated SQL data for demo |
| `data/` | SQLite database + uploaded images + thumbnails |
| `logs/` | Category-based log files (audit, bid, security, system, database) |
| `docs/` | Architecture, database, networking, design docs |
| `exports/` | Downloaded CSV exports |

### `src/main/java/com/auction/`

```
src/main/java/com/auction/
├── ApplicationInfo.java
├── TestLoad.java
├── shared/          ← Used by BOTH client and server
│   ├── Constants.java              — All magic values centralized
│   ├── interfaces/IAuctionService  — RMI contract (25+ methods)
│   ├── models/AuctionItem.java     — Serializable auction model
│   ├── models/Bid.java             — Serializable bid model
│   ├── models/User.java            — User model
│   ├── models/Admin.java           — Admin extends User
│   ├── enums/Category.java         — ELECTRONICS, FURNITURE, ART, OTHER
│   ├── enums/AuctionStatus.java    — ACTIVE, SOLD, EXPIRED, CANCELLED
│   └── exceptions/                 — 8 domain exception classes
│
├── server/
│   ├── core/
│   │   ├── ServerLauncher.java     — Entry point (main)
│   │   ├── ServerBootstrap.java    — DI wiring, RMI setup
│   │   ├── AuctionManager.java     — ★ CORE: bidding engine
│   │   ├── LifecycleManager.java   — ★ State machine transitions
│   │   ├── ImageStore.java         — ★ Image persistence + thumbnails
│   │   ├── AdminManager.java       — CSV, backup, audit logs
│   │   ├── LockManager.java        — Per-auction ReentrantLock
│   │   ├── TransactionManager.java — SQL transaction wrapper
│   │   ├── SessionContext.java     — Record(username, role)
│   │   ├── UdpBroadcaster.java     — UDP discovery broadcasts
│   │   └── logging/                — AsyncLogger, LogCategory, EventType, LogEntry
│   ├── service/
│   │   ├── AuctionServiceImpl.java — RMI remote object (thin adapter)
│   │   └── AuctionReaper.java      — Background 1s sweep
│   ├── repository/
│   │   ├── DatabaseManager.java    — SQLite connection + schema
│   │   ├── UserRepository.java     — User CRUD
│   │   ├── AuctionRepository.java  — Auction CRUD + search
│   │   └── BidRepository.java      — Bid CRUD
│   └── util/
│       ├── SecurityUtil.java       — SHA-256 hashing
│       ├── AuditLogger.java        — Legacy audit log
│       ├── AdminUtil.java          — Read audit log
│       └── CsvExportUtil.java      — CSV generation
│
├── client/
│   ├── core/
│   │   ├── ClientLauncher.java     — Entry point
│   │   └── ClientContext.java      — Singleton: session, RMI, nav, watcher
│   ├── ClientApp.java              — JavaFX Application, sets PrimerDark
│   ├── ui/ViewLoader.java          — FXML loading + scene switching
│   ├── network/
│   │   ├── RmiClientProvider.java  — RMI stub management
│   │   └── UdpDiscoveryClient.java — UDP server discovery listener
│   ├── service/
│   │   ├── PollingService.java     — ★ Configurable 1s polling + backoff
│   │   ├── BidHistoryService.java  — Async bid history loader
│   │   └── ThumbnailExecutor.java  — 4-thread pool for image loading
│   ├── controllers/
│   │   ├── ConnectController.java        — Server discovery / manual connect
│   │   ├── LoginController.java          — Username/password login
│   │   ├── RegistrationController.java   — Self-registration
│   │   ├── GalleryController.java        — Auction gallery grid (2s polling)
│   │   ├── AuctionDetailController.java  — ★ Single auction + bidding (1124 lines)
│   │   ├── AuctionBidHistoryController.java — Full bid history table
│   │   ├── UserDashboardController.java  — My activity, create/edit/relist
│   │   └── AdminPanelController.java     — User mgmt, backup, audit logs
│   └── util/
│       ├── MockDataGenerator.java  — 15 mock items for UI testing
│       └── Toast.java              — Toast notification
│
└── tools/
    ├── UdpDiscoveryListener.java   — Standalone discovery tool
    └── TestRegisterLogin.java      — Test register/login flow
```

### `src/main/resources/`

```
src/main/resources/
├── fxml/
│   ├── connect.fxml              — Server discovery screen
│   ├── login.fxml                — Login screen
│   ├── registration.fxml         — Registration screen
│   ├── gallery.fxml              — Auction gallery
│   ├── auction_detail.fxml       — Auction detail + bidding
│   ├── auction_bid_history.fxml  — Full bid history
│   ├── user_dashboard.fxml       — User dashboard (largest FXML)
│   └── admin_panel.fxml          — Admin panel
└── css/
    └── style.css                 — 656 lines of dark theme
```

### `src/test/java/com/auction/`

| Package | Test Files | What They Test |
|---------|------------|----------------|
| `server.repository` | `AuctionRepositoryTest`, `BidRepositoryTest`, `UserRepositoryTest`, `DatabaseManagerTest` | Repository CRUD operations, schema creation |
| `server.core` | `ScheduledAuctionLifecycleTest` | Scheduled→ACTIVE transition, manual start, snipe cap |
| `client.service` | `PollingServiceTest` | Failure notification and recovery |
| `client.controllers` | 10 files (FXML loads, bid UI, countdown, thumbnails, sort, navigation) | UI behavior and FXML loading |
| `integration` | `GalleryDetailBidIntegrationTest` | End-to-end: create → gallery → detail → bid |
| `stress` | `ConcurrentBiddingHighStressTest`, `ConcurrentBiddingAndPollingStressTest` | 10-thread concurrent bidding, polling shutdown |

---

## 4. Important Class Documentation

### 4.1 Shared Module

#### `IAuctionService` (`shared/interfaces/IAuctionService.java`)

**Responsibility:** The RMI remote interface. This is the **sole contract** between client and server.

**Key Methods (27 total):**

| Method | Signature | Purpose |
|--------|-----------|---------|
| `login` | `String login(String, String)` | Authenticate user, return session token |
| `register` | `void register(String, String, String)` | Create new user |
| `getActiveAuctions` | `List<AuctionItem> getActiveAuctions()` | Get ACTIVE + SCHEDULED auctions |
| `getAllAuctions` | `List<AuctionItem> getAllAuctions()` | Get all auctions regardless of status |
| `searchActiveAuctions` | `List<AuctionItem> searchActiveAuctions(String, String, String)` | Filter + sort active auctions (default method) |
| `placeBid` | `void placeBid(int, long, long, String)` | Place bid (auctionId, amountCents, expectedPriceCents, token) |
| `getAuctionById` | `AuctionItem getAuctionById(int)` | Get single auction detail |
| `createAuction` | `int createAuction(AuctionItem, byte[], byte[], byte[], String)` | Create auction with up to 3 images |
| `getThumbnail` | `byte[] getThumbnail(int, int)` | Get thumbnail bytes for LQIP |
| `getFullImage` | `byte[] getFullImage(int, int)` | Get full-resolution image bytes |
| `exportAuctionsToCSV` | `byte[] exportAuctionsToCSV(String)` | Download CSV of auctions |
| `backupDatabase` | `byte[] backupDatabase(String)` | Download database backup (admin only) |
| `getAuditLogs` | `List<String> getAuditLogs(int, String)` | Get last N audit log lines (admin only) |

**Why It Matters:** Every RMI call goes through this interface. Understanding it means understanding the entire feature set.

#### `AuctionItem` (`shared/models/AuctionItem.java`)

**Responsibility:** Serializable model representing a single auction listing.

**Key Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `id` | `int` | Auto-generated primary key |
| `title` | `String` | Auction title |
| `description` | `String` | Auction description |
| `category` | `String` | One of ELECTRONICS, FURNITURE, ART, OTHER |
| `startingPriceCents` | `long` | Starting price in cents |
| `currentBidCents` | `long` | Current highest bid in cents |
| `highestBidderUsername` | `String` | Current winner (null if no bids) |
| `sellerUsername` | `String` | Seller who created the auction |
| `startTime` | `String` | ISO-8601 UTC start time |
| `endTime` | `String` | ISO-8601 UTC end time |
| `capEndTime` | `String` | Snipe cap = endTime + 10 min |
| `status` | `String` | SCHEDULED, ACTIVE, SOLD, EXPIRED, CANCELLED |
| `startMode` | `String` | AUTO or MANUAL |
| `img1`, `img2`, `img3` | `String` | File paths (or null) for up to 3 images |
| `minIncrementPercent` | `double` | Minimum bid increment (default 0.05 = 5%) |
| `relistedFrom` | `Integer` | If relisted, the original auction ID |

**Money Convention:** All monetary values are **integer cents**. Display uses `Constants.formatCents()` which returns `"$%.2f"` from `cents / 100.0`. This avoids floating-point rounding errors.

#### `Bid` (`shared/models/Bid.java`)

**Responsibility:** Serializable model representing a single bid.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `int` | Auto-generated primary key |
| `auctionItemId` | `int` | FK to auction_items |
| `bidderUsername` | `String` | Who placed the bid |
| `amountCents` | `long` | Bid amount in cents |
| `timestamp` | `String` | ISO-8601 UTC timestamp |

#### `User` / `Admin` (`shared/models/`)

- `User`: username, passwordHash, roleType, createdAt. No setters — immutable after construction.
- `Admin extends User`: Hard-codes `roleType = "ADMIN"` in constructor.

#### `Constants` (`shared/Constants.java`)

**Responsibility:** Single source of truth for all magic values. **Memorize the key ones for presentation.**

| Constant | Value | Why It Matters |
|----------|-------|----------------|
| `DEFAULT_RMI_PORT` | `1099` | RMI registry port |
| `UDP_BROADCAST_PORT` | `9999` | UDP discovery port |
| `UDP_BROADCAST_INTERVAL_MS` | `2000` | Server broadcasts every 2s |
| `MIN_BID_INCREMENT_PERCENT` | `0.05` | 5% minimum bid increase |
| `SNIPE_PROTECTION_SECONDS` | `30` | Snipe threshold |
| `SNIPE_CAP_DEFAULT_MINUTES` | `10` | Max snipe extension |
| `REAPER_INTERVAL_SECONDS` | `1` | Lifecycle sweep interval |
| `CLIENT_POLL_INTERVAL_MS` | `2000` | Client polling interval |
| `MAX_IMAGE_SIZE_BYTES` | `2097152` | 2MB image size limit |
| `SESSION_TTL_MINUTES` | `30` | Session token expiry |
| `DEFAULT_ADMIN_USERNAME` | `"admin"` | Default admin credentials |
| `DEFAULT_ADMIN_PASSWORD` | `"admin"` | Default admin credentials |

#### Domain Exceptions

```
AuctionException (base)
├── AuctionClosedException    — Bid on non-ACTIVE or expired auction
├── DuplicateBidException     — Already highest bidder
├── InsufficientBidException  — Below minimum increment
├── SelfBidException          — Seller bids own auction
├── SnipeCapReachedException  — Snipe cap hit
├── StaleDataException        — Client price ≠ server price
└── UnauthorizedException     — Invalid session or role
```

### 4.2 Server Core

#### `AuctionManager` (`server/core/AuctionManager.java`) — ★ MOST IMPORTANT CLASS

**Responsibility:** The bidding engine. Enforces ALL domain rules.

**Dependencies:** `AuctionRepository`, `BidRepository`, `LockManager`, `TransactionManager`

**Key Write Methods:**

| Method | Lock | Transaction | Purpose |
|--------|------|-------------|---------|
| `placeBid` | ✅ | ✅ | Core bidding logic with all 6 validations |
| `createAuction` | ❌ | ✅ | Create new auction listing |
| `updateAuction` | ✅ | ✅ | Edit SCHEDULED auction (no bids yet) |
| `cancelAuction` | ✅ | ✅ | Cancel ACTIVE auction (no bids only) |
| `relistAuction` | ✅ | ✅ | Create new auction from expired/cancelled one |
| `startAuction` | ✅ | ✅ | Manually start SCHEDULED auction (5-min window) |

**Presentation Talking Points:**
- "AuctionManager is the heart of the system. It enforces all 6 bidding rules."
- "Every write operation uses per-auction locking + database transactions for safety."
- "The `placeBid` method has 6 validation checks before accepting a bid."

#### `LifecycleManager` (`server/core/LifecycleManager.java`)

**Responsibility:** State machine. Transitions auctions between states.

**Key Methods:**

| Method | Purpose |
|--------|---------|
| `sweepOverdue()` | Finds ACTIVE auctions past endTime → SOLD (has bids) or EXPIRED (no bids) |
| `activateScheduled()` | Starts SCHEDULED auctions whose startTime has arrived; cancels overdue manual-start auctions |

**Important detail:** Both methods re-fetch the auction inside the lock before modifying. This **double-check pattern** prevents race conditions.

#### `LockManager` (`server/core/LockManager.java`)

**Responsibility:** Per-auction `ReentrantLock` management via `ConcurrentHashMap<Integer, ReentrantLock>`.

**Why per-auction locks instead of a global lock?** Higher concurrency. Independent auctions don't block each other.

#### `TransactionManager` (`server/core/TransactionManager.java`)

**Responsibility:** Wraps SQL operations in `autoCommit=false` → `commit()` / `rollback()`.

**Why transactions?** A single `placeBid` inserts a bid row, updates the auction's price, and potentially updates the end time (snipe). These must be atomic — if the server crashes mid-operation, the bid is rolled back.

#### `ImageStore` (`server/core/ImageStore.java`)

**Responsibility:** Save, load, and generate thumbnails for auction images.

**Key Flow:**
1. `stageImages(byte[] i1, i2, i3)` — saves up to 3 images to `data/images/` with UUID filenames, generates 360px thumbnails in `data/thumbs/`
2. Image processing: re-encodes to JPEG, strips EXIF, validates size ≤ 2MB and dimensions ≤ 2000×2000
3. Multi-path resolution for loading: tries absolute → CWD-relative → parent-relative → project-subfolder-relative

#### `AuctionServiceImpl` (`server/service/AuctionServiceImpl.java`)

**Responsibility:** The RMI remote object. A **thin adapter** that validates sessions and delegates to managers.

**Session Management:**
- Uses `ConcurrentHashMap<String, SessionInfo>` for session tokens
- `SessionInfo` contains `SessionContext` (username, role) + expiry time
- TTL = 30 minutes, extended on each use
- Validated on every RMI call that requires authentication

#### `AuctionReaper` (`server/service/AuctionReaper.java`)

**Responsibility:** Background daemon that runs every 1 second.

**Key Methods:**
- `start()` — begins periodic `activateScheduled()` + `sweepOverdue()`
- `recoverFromCrash()` — called at startup, runs `sweepOverdue()` to catch auctions that expired while server was down
- `stop()` — shuts down the scheduler

#### `AsyncLogger` (`server/core/logging/AsyncLogger.java`)

**Responsibility:** Singleton async logger. Non-blocking `log()` calls.

**How it works:**
1. `log()` → puts `LogEntry` on a `BlockingQueue<LogEntry>`
2. Daemon worker thread takes from queue and writes to category-specific files
3. Files: `logs/audit.log`, `logs/bid.log`, `logs/security.log`, `logs/system.log`, `logs/database.log`
4. JVM shutdown hook drains remaining entries

#### `DatabaseManager` (`server/repository/DatabaseManager.java`)

**Responsibility:** SQLite connection, schema creation, and legacy migrations.

**What happens on first run:**
1. Creates directories: `data/`, `logs/`, `resources/images/`, `resources/thumbs/`, `exports/`
2. Copies legacy `auction.db` → `auction.db.sqlite` if needed
3. Opens SQLite connection with `PRAGMA foreign_keys = ON`
4. Runs column migrations (adds `start_mode`, `min_increment_percent` columns)
5. Creates tables and indexes if they don't exist

### 4.3 Client Core

#### `ClientContext` (`client/core/ClientContext.java`)

**Responsibility:** Singleton holding all global client state.

**What it holds:**
- `RmiClientProvider` — RMI stub
- `UdpDiscoveryClient` — UDP listener
- `ViewLoader` — FXML scene manager
- Session token, user role, username
- Current auction ID, previous view name
- Auto-launch watcher (background thread that detects SCHEDULED → ACTIVE transitions)

**Key methods:**
- `setSessionToken()` — side effect: starts auto-launch watcher
- `clearSession()` — stops watcher, clears all fields
- `handleConnectionLost()` — resets RMI provider, navigates to connect screen

#### `PollingService` (`client/service/PollingService.java`)

**Responsibility:** Configurable polling with exponential backoff.

**Key features:**
- Default: 1s interval, 3 failures before notification, 32s max backoff
- `startPolling(auctionId, onUpdate, onFailure)` — begins polling loop
- `pause()` / `resume()` — for window focus management
- Used by `AuctionDetailController` to poll `getAuctionById()` every 1s

**Exponential backoff formula:** On failure, wait = `min(base * 2^failures, maxBackoff)`. On success, reset failures to 0.

### 4.4 Client Controllers

#### `AuctionDetailController` (`client/controllers/AuctionDetailController.java`) — ★ 1124 lines

**Responsibility:** The most complex controller. Manages auction detail view, bidding, countdown, images, and polling.

**Key Methods:**

| Method | Purpose |
|--------|---------|
| `loadAuction(int)` | Async load auction + images + start polling |
| `handlePlaceBid()` | Optimistic UI + async RMI call + rollback on failure |
| `updateUi(AuctionItem)` | Update all labels, images, and status |
| `startCountdownTicker()` | JavaFX Timeline — 1s countdown with color coding |
| `syncServerClockOffset()` | Call `serverTime()` to offset client clock drift |
| `promoteThumbnailToHero(int)` | Click thumbnail to show as hero image |
| `refreshRecentBids()` | Async bid history loading with overlap prevention |

**Optimistic Bidding Flow:**
```
1. User clicks "Place Bid"
2. Controller immediately updates currentBid + highestBidder in local model
3. Disables bid button, shows spinner
4. Calls service.placeBid() asynchronously
5a. SUCCESS: Clear field, show toast, animate hero image bounce
5b. FAILURE: Roll back local model to previous values, show error Alert
6. Re-enable button in finally block
```

**Countdown Color Coding:**
| Remaining Time | Color | Visual Effect |
|----------------|-------|---------------|
| > 60 seconds | `#8b949e` (gray) | Normal |
| 30-60 seconds | `#d29922` (yellow/warning) | Yellow highlight |
| < 30 seconds | `#f85149` (red) | Pulsing animation |

#### `GalleryController` (`client/controllers/GalleryController.java`)

**Responsibility:** Browse auctions in a grid with filtering, sorting, and polling.

**Key Features:**
- Fetches `getAllAuctions()` every 2 seconds
- Client-side filter: status, category, search query
- Client-side sort: newest, price asc/desc
- Thumbnail loading with `ConcurrentHashMap` cache
- Each auction card: thumbnail (220×140), title, price, status chip, "View" button

#### `UserDashboardController` (`client/controllers/UserDashboardController.java`)

**Responsibility:** The main screen for regular users. Dashboard + auction management.

**Key Features:**
- 4 tabs: Browse Auctions, My Listings (filterable by status), My Bids, Won Auctions
- Create auction form: title, description, category, prices, start/end time, up to 3 images, start mode (auto/manual)
- Edit SCHEDULED auctions, relist expired/cancelled auctions, start scheduled auctions
- CSV export of own listings
- Metric cards: total sales, counts

#### `AdminPanelController` (`client/controllers/AdminPanelController.java`)

**Responsibility:** System administration.

**Key Features:**
- 3 sections: Users (searchable table + create user), Auctions (table with thumbnails + actions), Audit Logs
- Database backup: calls `backupDatabase()`, saves as `backup_TIMESTAMP.db`
- Action buttons per auction: View Details, Cancel, Relist

### 4.5 Client Services

#### `ThumbnailExecutor` (`client/service/ThumbnailExecutor.java`)

**Responsibility:** Fixed 4-thread daemon pool for async image loading.

**Why 4 threads?** Enough for concurrent thumbnail loading without overwhelming the client. Daemon threads don't prevent JVM shutdown.

#### `BidHistoryService` (`client/service/BidHistoryService.java`)

**Responsibility:** Static utility returning `CompletableFuture<List<Bid>>` for async bid history loading.

---

## 5. Important Function Documentation

### 5.1 `AuctionManager.placeBid()` — ★ THE MOST IMPORTANT FUNCTION

```java
public void placeBid(int auctionId, SessionContext user,
                     long amountCents, long clientExpectedPriceCents)
```

**What it does:** Places a bid on an auction. This is the core transaction of the entire system.

**Flow (inside lock + transaction):**

| Step | Method | Purpose | Exception on Failure |
|------|--------|---------|---------------------|
| 1 | `validateActive(item)` | Status must be ACTIVE | `AuctionClosedException` |
| 2 | `validateNotSeller(item, bidder)` | Seller can't bid own auction | `SelfBidException` |
| 3 | `validateNotCurrentWinner(item, bidder)` | Current winner can't re-bid | `DuplicateBidException` |
| 4 | `validateFreshness(item, expectedCents)` | Client's price must match server's | `StaleDataException` |
| 5 | `validateMinimumBid(item, amountCents)` | Must be ≥ currentPrice + 5% | `InsufficientBidException` |
| 6 | `validateNotExpired(item)` | Must be before endTime | `AuctionClosedException` |
| 7 | `applySnipeProtection(item, now)` | Extend if < 30s left | — |
| 8 | Insert bid + update auction + maybe extend end time | — | SQLException → rollback |

**Validation 4 — Stale Data Detection (Optimistic Locking):**
```
Client knows: currentBid = $142.00
Client sends: placeBid(42, 15000, 14200, token)
                                    ↑
                            expectedPriceCents
Server checks: does 14200 == currentBidCents?
- If YES: proceed (no one else bid since you loaded)
- If NO: throw StaleDataException (someone else bid first!)
```

**Validation 5 — Minimum Increment:**
```
If no bids yet: amountCents >= startingPriceCents
If bids exist:  amountCents >= currentBidCents + max(1, currentBid * 0.05)
                e.g., currentBid = $100.00 → minimum next bid = $105.00
```

**Snipe Protection Logic:**
```java
if (remaining seconds < SNIPE_PROTECTION_SECONDS) {  // 30s
    newEndTime = min(now + 30s, capEndTime);          // capEndTime = originalEndTime + 10min
}
```

### 5.2 `AuctionManager.createAuction()`

```java
public int createAuction(AuctionItem item, SessionContext user, String[] imagePaths)
```

**Logic:**
1. Sets seller from session
2. Determines initial status:
   - `MANUAL` start mode → always SCHEDULED
   - `AUTO` start mode → SCHEDULED if startTime in future, else ACTIVE
3. If immediately ACTIVE: sets `currentBidCents` = `startingPriceCents`
4. Computes `capEndTime` = `endTime + SNIPE_CAP_DEFAULT_MINUTES`
5. Attaches image paths, inserts into database

### 5.3 `AuctionManager.relistAuction()`

```java
public int relistAuction(int auctionId, String newEndTimeIso, SessionContext user)
```

**Logic:**
1. Validates parent auction exists and seller owns it
2. Parent must be EXPIRED or CANCELLED
3. Prevents duplicate relisting (checks if any auction already has `relisted_from = auctionId`)
4. Creates new child auction copying title, description, category, starting price, images
5. Sets `relistedFrom = auctionId` on the child
6. New auction is SCHEDULED with MANUAL start mode

### 5.4 `LifecycleManager.sweepOverdue()`

```java
public void sweepOverdue()
```

**Logic:**
1. Queries `findActiveExpiredAuctions(now)` — finds all ACTIVE auctions past endTime
2. For each: acquire lock → re-fetch (double-check) → if still ACTIVE and expired:
   - Has bids → SOLD
   - No bids → EXPIRED
3. Logs each transition, catches exceptions per-item

### 5.5 `TransactionManager.execute()`

```java
public <T> T execute(TransactionCallback<T> action) throws Exception
```

**Logic:**
```
setAutoCommit(false)
try {
    T result = action.doInTransaction()
    commit()
    return result
} catch (Exception e) {
    rollback()
    throw e
} finally {
    setAutoCommit(true)
}
```

### 5.6 `ImageStore.normalizeImage()`

**Logic:**
1. Validate size ≤ 2MB
2. Detect format (JPG/JPEG/PNG only) via `ImageIO.createImageInputStream`
3. Validate dimensions ≤ 2000×2000
4. Convert to `TYPE_INT_RGB` (handles transparency)
5. Generate 360px thumbnail (bilinear interpolation, proportional scaling)
6. Return `ImagePayload` with JPEG-encoded full + thumb bytes

### 5.7 `PollingService.startPolling()`

```java
public void startPolling(int auctionId, Consumer<AuctionItem> onUpdate,
                         Consumer<Throwable> onFailure)
```

**Logic:**
1. Creates single-thread `ScheduledExecutorService`
2. First poll immediately (delay=0), then at `baseIntervalSeconds`
3. On success: invoke `onUpdate`, schedule next poll
4. On failure: increment counter, if ≥ `maxFailuresBeforeNotify` → invoke `onFailure`, do exponential backoff
5. `pause()`: skip RMI calls while keeping scheduler alive
6. `shutdown()`: cancel all pending tasks

### 5.8 `AuctionDetailController.handlePlaceBid()`

**Logic (simplified):**
```java
long newAmount = parseCents(bidAmountField.getText());
long expectedPrice = currentItem.getCurrentBidCents();

// 1. Optimistic update
currentItem.setCurrentBidCents(newAmount);
currentItem.setHighestBidderUsername(context.getUsername());
updateUi(currentItem);

// 2. Disable controls
placeBidButton.setDisable(true);
bidSpinner.setVisible(true);

// 3. Async RMI call
CompletableFuture.runAsync(() -> {
    service.placeBid(auctionId, newAmount, expectedPrice, token);
}).thenRun(() -> Platform.runLater(() -> {
    // SUCCESS: toast, animation, refresh
})). exceptionally(ex -> {
    Platform.runLater(() -> {
        // FAILURE: rollback optimistic update, show Alert
    });
    return null;
}).whenComplete((_, _) -> Platform.runLater(() -> {
    placeBidButton.setDisable(false);
    bidSpinner.setVisible(false);
}));
```

---

## 6. Networking & Communication Flow

### 6.1 How RMI Works in This Project

**RMI (Remote Method Invocation)** is Java's built-in mechanism for calling methods on objects in a different JVM.

**Server Setup:**
```java
// ServerBootstrap
AuctionServiceImpl service = new AuctionServiceImpl(...);
Registry registry = LocateRegistry.createRegistry(1099);
registry.rebind("AuctionService", service);
```

**Client Setup:**
```java
// RmiClientProvider
Registry registry = LocateRegistry.getRegistry(host, 1099);
IAuctionService service = (IAuctionService) registry.lookup("AuctionService");
```

**What Makes It Work:**
- `IAuctionService extends Remote` — marks the interface as remotely callable
- Every method throws `RemoteException` — the network error type
- All models (`AuctionItem`, `Bid`, `User`) implement `Serializable` — they're serialized and deserialized across the network
- `AuctionServiceImpl extends UnicastRemoteObject` — exports the object for remote access

**Request Lifecycle:**
```
1. Client calls service.placeBid(42, 15000, 14200, "token-abc")
2. RMI serializes the arguments (int, long, long, String)
3. Network transport sends to server
4. Server deserializes, looks up the method, executes
5. Return value (or exception) is serialized back
6. Client deserializes the result
```

### 6.2 UDP Discovery Protocol

**Why UDP?** Server discovery is inherently broadcast. No persistent connection needed.

**Server Side** (`UdpBroadcaster`):
- Every 2 seconds: opens `DatagramSocket`, creates packet with `RTDAS|MainServer|1099`
- Sends to `255.255.255.255` (broadcast address) on port 9999

**Client Side** (`UdpDiscoveryClient`):
- Binds `DatagramSocket` on port 9999 with `SO_REUSEADDR`
- Background daemon thread loops: `socket.receive(packet)` → parse → store in `CopyOnWriteArrayList<ServerInfo>`
- Packet format: `RTDAS|<serverName>|<rmiPort>`

**Client UI** (`ConnectController`):
- 1-second daemon thread polls `getDiscoveredServers()` and updates ListView via `Platform.runLater()`
- User clicks a server → IP/port fields auto-fill
- "Connect" button → `RmiClientProvider.connect(host, port)` → navigate to login

### 6.3 Why Polling Instead of Push (RMI Callbacks)

| Approach | Why Rejected |
|----------|--------------|
| RMI Callbacks | Client would need to export its own RMI object. More complex. Firewall issues. |
| WebSockets | Not a standard Java API. Would add dependencies. |
| Polling | Simple, reliable, fits demo scale. Server is stateless for reads. |

### 6.4 Server Clock Synchronization

Clients compute a clock offset to show accurate countdowns:

```java
// Client calls:
String serverTimeIso = service.serverTime();
long serverTimeMs = Instant.parse(serverTimeIso).toEpochMilli();
long clientTimeMs = System.currentTimeMillis();
long offsetMs = serverTimeMs - clientTimeMs;

// Then for countdown:
long adjustedNow = System.currentTimeMillis() + offsetMs;
long remaining = endTimeMs - adjustedNow;
```

This handles cases where the client's system clock is wrong.

### 6.5 Reconnection Strategy

1. `PollingService` detects 3 consecutive failures → invokes `onFailure` callback
2. `AuctionDetailController.showReconnectBanner()` — shows "Connection lost" HBox
3. User can navigate back to connect screen
4. `ClientContext.handleConnectionLost()` — resets RMI provider, loads `connect.fxml`

---

## 7. Database & System State

### 7.1 Storage Strategy

- **Engine:** SQLite — embedded, zero-configuration, single-file
- **Connection:** Single shared `Connection` in `DatabaseManager` (not a connection pool — SQLite is single-writer)
- **File:** `data/auction.db.sqlite`

### 7.2 Schema

**Three tables:**

#### `users`
| Column | Type | Notes |
|--------|------|-------|
| username | TEXT PK | Login name |
| password_hash | TEXT NOT NULL | SHA-256 hex (64 chars) |
| role | TEXT NOT NULL CHECK | 'ADMIN' or 'USER' |
| created_at | TEXT NOT NULL | ISO-8601 UTC |

#### `auction_items`
| Column | Type | Notes |
|--------|------|-------|
| id | INTEGER PK AUTO | |
| title | TEXT NOT NULL | |
| description | TEXT | Optional |
| category | TEXT NOT NULL | ELECTRONICS/FURNITURE/ART/OTHER |
| starting_price_cents | INTEGER NOT NULL | ≥ 0 |
| current_bid_cents | INTEGER NOT NULL | ≥ 0 |
| highest_bidder_username | TEXT | FK → users.username, nullable |
| seller_username | TEXT NOT NULL | FK → users.username |
| start_time | TEXT NOT NULL | ISO-8601 |
| end_time | TEXT NOT NULL | ISO-8601 |
| cap_end_time | TEXT | Snipe limit |
| status | TEXT NOT NULL | SCHEDULED/ACTIVE/SOLD/EXPIRED/CANCELLED |
| start_mode | TEXT NOT NULL DEFAULT 'AUTO' | AUTO/MANUAL |
| min_increment_percent | REAL NOT NULL | Default 0.05 |
| img1, img2, img3 | TEXT | File paths |
| relisted_from | INTEGER | FK → auction_items.id |

#### `bids`
| Column | Type | Notes |
|--------|------|-------|
| id | INTEGER PK AUTO | |
| auction_item_id | INTEGER NOT NULL | FK → auction_items ON DELETE CASCADE |
| bidder_username | TEXT NOT NULL | FK → users.username |
| amount_cents | INTEGER NOT NULL | > 0 |
| timestamp | TEXT NOT NULL | ISO-8601 |

**Indexes:**
- `idx_bids_auction_id ON bids(auction_item_id)` — fast bid history lookups
- `idx_auction_status_end ON auction_items(status, end_time)` — reaper queries
- `idx_auction_seller ON auction_items(seller_username)` — user dashboard

### 7.3 Why INTEGER Cents (Critical Interview Question)

```
DOUBLE approach:    0.1 + 0.2 = 0.30000000000000004  ← BAD for money!
INTEGER approach:  10 + 20 = 30                      ← EXACT
```

All monetary values stored as `INTEGER` cents. Display converts: `String.format("$%.2f", cents / 100.0)`.

### 7.4 Transaction Flow for Bids

```sql
-- Within a single SQLite transaction:
BEGIN TRANSACTION;
INSERT INTO bids (auction_item_id, bidder_username, amount_cents, timestamp)
    VALUES (42, 'alice', 15000, '2026-05-29T10:00:00Z');
UPDATE auction_items
    SET current_bid_cents = 15000, highest_bidder_username = 'alice'
    WHERE id = 42;
UPDATE auction_items
    SET end_time = '2026-05-29T10:00:30Z'  -- snipe extension
    WHERE id = 42;
COMMIT;  -- or ROLLBACK on error
```

### 7.5 Backup Strategy

SQLite's `VACUUM INTO` command creates a clean copy of the database atomically:

```java
stmt.execute("VACUUM INTO 'data/backup_" + UUID + ".db'");
// Read backup file as byte[], return over RMI
// Delete temp file
```

### 7.6 Database Initialization

On first run, `DatabaseManager`:
1. Creates all required directories
2. Copies legacy `auction.db` → `auction.db.sqlite` if needed
3. Enables `PRAGMA foreign_keys = ON`
4. Runs column migrations (adds `start_mode`, `min_increment_percent`)
5. Creates tables and indexes
6. Seeds default admin user (`admin`/`admin`) if empty

---

## 8. Concurrency & Multithreading

### 8.1 Thread Map

| Thread/Pool | Count | Type | Created By | Purpose |
|-------------|-------|------|------------|---------|
| RMI request handler | Dynamic | Daemon? | JVM/RMI | Handles incoming RMI calls |
| `AuctionReaper` | 1 | Daemon | `ServerBootstrap` | 1s lifecycle sweep |
| `UdpBroadcaster` | 1 | Daemon | `ServerBootstrap` | 2s broadcast tick |
| `LoggerWorker` | 1 | Daemon | `AsyncLogger` | Drains log queue to files |
| `ThumbnailExecutor` | 4 | Daemon | `ClientApp` | Async image loading |
| `auto-launch-watcher` | 1 | Daemon | `ClientContext` | 2s poll for scheduled→active |
| UDP discovery listener | 1 | Daemon | `UdpDiscoveryClient` | Receive server broadcasts |
| Discovery UI updater | 1 | Daemon | `ConnectController` | 1s refresh of server list |
| `gallery-poller` | 1 | Daemon | `GalleryController` | 2s gallery polling |
| `PollingService` | 1 per view | Daemon | `AuctionDetailController` | 1s auction polling |
| Detail page executor | Cached | Daemon | `AuctionDetailController` | Async RMI calls |
| Dashboard refresh | 1 per refresh | Daemon | `UserDashboardController` | Load 4 data sets |

### 8.2 Server-Side Synchronization Strategy

**Per-auction `ReentrantLock`** (`LockManager`):
- Each auction has its own lock in a `ConcurrentHashMap<Integer, ReentrantLock>`
- `placeBid()` and `sweepOverdue()` acquire the lock for the specific auction
- Independent auctions never block each other
- Locks are created on-demand (`computeIfAbsent`)

**Database Transactions** (`TransactionManager`):
- `setAutoCommit(false)` → execute → `commit()` or `rollback()`
- Ensures atomicity of multi-statement operations

**Double-Check Pattern** in `LifecycleManager`:
```java
lockManager.lock(auctionId);
try {
    txManager.executeWithoutResult(() -> {
        AuctionItem fresh = auctionRepo.findAuctionById(auctionId);
        // Re-check inside lock + transaction
        if (!"ACTIVE".equals(fresh.getStatus())) return;
        // ... proceed with transition
    });
} finally {
    lockManager.unlock(auctionId);
}
```

### 8.3 Client-Side Synchronization

**`Platform.runLater()`** — All UI updates from background threads go through this.

**AtomicBoolean** in `AuctionDetailController.refreshRecentBids()`:
```java
if (!refreshing.compareAndSet(false, true)) return; // already refreshing
// ... async load
// in callback: refreshing.set(false);
```

**Optimistic UI with Rollback:**
- Client updates the UI immediately when user clicks "Place Bid"
- If server rejects the bid, client restores the previous state
- This gives instant feedback while maintaining correctness

### 8.4 Race Conditions Prevented

| Scenario | Prevention |
|----------|------------|
| Two bidders bid simultaneously on same auction | Per-auction `ReentrantLock` — second bidder waits |
| Reaper sells auction while bid is being placed | Per-auction lock — reaper waits for bid, or vice versa |
| Server crashes mid-bid | Transaction rollback — no partial state |
| Client shows stale price | Stale data detection — server rejects with `StaleDataException` |
| Auction extended by snipe while another bid arrives | Both inside same lock + transaction |

---

## 9. Frontend/UI Flow

### 9.1 Screen Flow

```
┌──────────┐     ┌─────────┐     ┌──────────────────┐
│ Connect   │────►│  Login   │────►│  User Dashboard  │───→ Gallery → Detail
│ (discover │     │         │     │  (4 tabs + form)  │     (2s)     (1s polling)
│  server)  │     └─────────┘     └──────────────────┘
└──────────┘          │                    │
                      │ (admin role)       │ (back)
                      ▼                    │
              ┌──────────────┐             │
              │ Admin Panel  │◄────────────┘
              │ Users/Auction│
              │ /Logs/Backup │
              └──────────────┘
```

### 9.2 Event Flow (Bidding Example)

```
1. User clicks "Place Bid" button
       │
2. AuctionDetailController.handlePlaceBid()
       │
3. Optimistic UI update: currentBidLabel.setText("$150.00")
                          highestBidderLabel.setText("alice")
       │
4. Disable button, show spinner
       │
5. CompletableFuture.runAsync() → RMI call
       │
6. Server processes (AuctionManager.placeBid)
       │
7a. SUCCESS → Platform.runLater:
    │   - Toast.show("Bid placed!")
    │   - animateSuccess() (hero image scale bounce)
    │   - refreshRecentBids()
    │
7b. FAILURE → Platform.runLater:
    │   - Rollback optimistic update
    │   - Show Alert (specific exception message)
    │
8. Re-enable button, hide spinner
```

### 9.3 Admin vs User Workflow

**User:**
1. Connect → Login → Dashboard (4 tabs)
2. Create auctions from sidebar form
3. Browse gallery → click auction → view detail
4. Place bids, edit SCHEDULED auctions, relist expired ones
5. CSV export of own listings

**Admin:**
1. Connect → Login → Admin Panel
2. View/search all users, create new users
3. View all auctions with thumbnails
4. Cancel/relist auctions (same as user)
5. Backup database
6. View audit logs

### 9.4 UI State Updates

**Gallery polling (2s):**
```
Timer → getAllAuctions() → renderAuctions(list)
  Each card: update title, price, countdown, status chip
  Thumbnails: check cache → load async → set ImageView
```

**Detail polling (1s):**
```
Timer → getAuctionById(id) → updateUi(item)
  Update: currentBid, highestBidder, countdown, status
  Check: endTime changed? (snipe extension → highlight)
  Refresh: recent bids table
```

**Countdown ticker (1s, on JavaFX Thread):**
```
Timeline → updateCountdownLabel()
  Compute: remaining = endTime - (clientNow + serverOffset)
  If > 60s: gray, normal
  If 30-60s: yellow warning
  If < 30s: red pulsing
  If ≤ 0: show "Auction ended" Alert with winner info
```

### 9.5 LQIP (Low-Quality Image Placeholder) Pattern

1. **Upload:** Server re-encodes to JPEG, strips EXIF, generates 360px thumbnail
2. **Gallery:** 40px blurred placeholder loaded first, then full-res in background
3. **Detail:** Image 1 loads automatically, images 2-3 load on click (promote to hero)
4. **Fallback:** Missing images show placeholder icon

---

## 10. Security & Validation

### 10.1 Authentication

**Password Hashing:**
```java
// SecurityUtil.hashPassword()
MessageDigest.getInstance("SHA-256").digest(password.getBytes());
// Returns 64-char hex string
```

**Note:** No salt is used. This is acceptable for a university demo but would need bcrypt/scrypt/Argon2 for production.

**Session Tokens:**
```java
String token = UUID.randomUUID().toString();
sessions.put(token, new SessionInfo(context, expiresAt));
// TTL: 30 minutes, extended on each use
```

**Default Admin:**
- Username: `admin`
- Password: `admin`
- Auto-seeded if users table is empty

### 10.2 Authorization

**Role-based access control on every RMI method:**
```java
// In AuctionServiceImpl:
SessionContext ctx = validateRole(token, "ADMIN");  // admin only
SessionContext ctx = validateRole(token, "USER");    // user only
SessionContext ctx = validateRole(token, "ADMIN", "USER"); // any authenticated
```

**Who Can Do What:**

| Operation | User | Admin |
|-----------|------|-------|
| Browse auctions | ✅ | ✅ |
| Place bids | ✅ | ❌ |
| Create auctions | ✅ | ❌ |
| Cancel/relist own | ✅ | ✅ |
| Edit own SCHEDULED | ✅ | ✅ |
| CSV export own | ✅ | ✅ |
| User management | ❌ | ✅ |
| Database backup | ❌ | ✅ |
| View audit logs | ❌ | ✅ |

### 10.3 Input Validation

**Client-Side:**
- Image size ≤ 2MB, format JPG/JPEG/PNG
- End time must be after start time
- Non-empty title, category
- Bid amount must be positive number

**Server-Side (in `ImageStore.normalizeImage()`):**
- Size ≤ `MAX_IMAGE_SIZE_BYTES` (2MB)
- Format JPG/JPEG/PNG via `ImageIO` format detection
- Dimensions ≤ 2000×2000

**Server-Side (in `AuctionManager`):**
- All 6 bid validations (active, not seller, not winner, freshness, minimum, not expired)
- Authorization checks in `AuctionServiceImpl`

### 10.4 SQL Injection Prevention

All database queries use **parameterized prepared statements**:
```java
// ✅ Safe
PreparedStatement ps = connection.prepareStatement(
    "SELECT * FROM auction_items WHERE id = ?");
ps.setInt(1, auctionId);

// NOT used:
// ❌ Unsafe: "SELECT * FROM auction_items WHERE id = " + auctionId
```

### 10.5 Snipe Protection

Prevents "sniping" (bidding at the last second to prevent competition):
- If a bid is placed within 30 seconds of `endTime`, extend `endTime` by 30 seconds
- Capped at `capEndTime` = original `endTime + 10 minutes`
- Prevents auctions from running forever

---

## 11. Execution Flow

### 11.1 Server Startup

```
ServerLauncher.main()
  │
  ▼
resolveRmiPort(args) → defaults to 1099
  │
  ▼
InetAddress.getLocalHost().getHostAddress() → sets java.rmi.server.hostname
  │
  ▼
new ServerBootstrap(rmiPort)
  │
  ├── AsyncLogger.initialize()           — Start async logger
  ├── new DatabaseManager()              — SQLite connection + schema
  ├── new UserRepository(conn)           — Seeds default admin
  ├── new AuctionRepository(conn)
  ├── new BidRepository(conn)
  ├── new TransactionManager(conn)
  ├── new LockManager()
  ├── new AuctionManager(...)            — Core bidding engine
  ├── new LifecycleManager(...)          — State machine
  ├── new ImageStore()                   — Image persistence
  ├── new AdminManager(...)
  ├── new AuctionServiceImpl(...)        — RMI remote object
  ├── LocateRegistry.createRegistry(1099)
  ├── registry.rebind("AuctionService", service)
  ├── new AuctionReaper(lifecycleManager)
  └── new UdpBroadcaster(1099, "MainServer")
  │
  ▼
bootstrap.start()
  ├── reaper.recoverFromCrash()          — Sweep overdue auctions
  ├── reaper.start()                     — Begin 1s lifecycle cycle
  └── broadcaster.start()                — Begin 2s UDP broadcasts
  │
  ▼
"Server is ready." (console output)
```

### 11.2 Client Startup

```
ClientLauncher.main()
  │
  ▼
Application.launch(ClientApp.class)
  │
  ▼
ClientApp.start(stage)
  │
  ├── Application.setUserAgentStylesheet("primer-dark.css")
  ├── new ViewLoader(stage)
  ├── ClientContext.getInstance().setViewLoader(viewLoader)
  └── viewLoader.loadView("connect.fxml") → ConnectController
  │
  ▼
ConnectController.initialize()
  ├── UdpDiscoveryClient.startListening()  — UDP listener thread
  ├── Background thread: poll discovered servers every 1s
  └── ListView selection listener → auto-fill IP/port
  │
  ▼
User clicks "Connect" → RmiClientProvider.connect(host, 1099)
  │
  ▼
viewLoader.loadView("login.fxml") → LoginController
  │
  ▼
User enters credentials → service.login(username, password)
  │
  ▼
Session token stored in ClientContext
  │
  ▼
if (role == "ADMIN") → admin_panel.fxml
if (role == "USER")  → user_dashboard.fxml
```

### 11.3 User Request Lifecycle (Full Bid Example)

```
1. User fills bid amount → clicks "Place Bid"
2. AuctionDetailController.handlePlaceBid()
3. Optimistic UI: update price label immediately
4. Disable button, show spinner
5. CompletableFuture.runAsync(() →
     service.placeBid(auctionId, amountCents, expectedPriceCents, token))
6. RMI serializes call → network → server
7. AuctionServiceImpl.placeBid()
     → validateSession(token) → SessionContext
     → validateRole(token, "USER")
     → auctionManager.placeBid(auctionId, ctx, amountCents, expectedPriceCents)
8. AuctionManager.placeBid():
     a. LockManager.lock(auctionId) — acquire per-auction lock
     b. TransactionManager.executeWithoutResult(() → {
          1. AuctionRepository.findAuctionById(auctionId)
          2. validateActive()       — status check
          3. validateNotSeller()    — self-bid check
          4. validateNotCurrentWinner() — duplicate check
          5. validateFreshness()    — stale data check
          6. validateMinimumBid()   — 5% increment check
          7. validateNotExpired()   — time check
          8. applySnipeProtection() — extend if < 30s
          9. BidRepository.insertBid(bid)
         10. AuctionRepository.updateAuctionBid(price, winner)
         11. AuctionRepository.updateAuctionEndTime(newEnd) // if snipe
         12. AsyncLogger.log(BID, PLACE_BID, ...)
        })
     c. LockManager.unlock(auctionId) — release lock
9. Return (void) serialized back to client
10. CompletableFuture callback on client:
     SUCCESS → Platform.runLater:
         Toast.show("Bid placed!")
         animateSuccess()
         refreshRecentBids()
     FAILURE → Platform.runLater:
         Rollback optimistic UI
         Show appropriate Alert
11. Re-enable button, hide spinner
```

---

## 12. Presentation Preparation Notes

### 12.1 Amira — Project Overview & Architecture

**Key Talking Points:**
- "RTDAS is a distributed English auction platform — like eBay but for a LAN"
- "Built to demonstrate OOP, collections, multithreading, file I/O, JDBC, and RMI"
- "Deep Module Architecture: core logic lives in managers, not in networking code"
- "The RMI service is intentionally thin — it just validates sessions and delegates"

**Possible Instructor Questions:**
- *Why Deep Module Architecture?* "Bidding rules (like the 5% increment) are in one place — AuctionManager. If we replaced RMI with sockets, the rules stay the same."
- *Why RMI instead of REST/HTTP?* "RMI is built into Java, needs no extra libraries, and handles object serialization automatically. Perfect for a LAN demo."
- *What's the difference between checked and unchecked exceptions?* "AuctionException is checked — callers must handle it. RuntimeException is unchecked."
- *Explain Java serialization.* "Objects implement Serializable. RMI serializes them to byte streams, sends over the network, deserializes on the other side."

**Simplify If Asked:**
- "Think of the server as a back-end with three layers: thin RMI wrapper → smart managers → database"
- "The shared module is like a contract both sides agree on — models, interface, exception types"

### 12.2 Barok — Server Core & Domain Logic

**Key Talking Points:**
- "AuctionManager is the most important class — it's the bidding engine with 6 validation checks"
- "Every bid goes through: lock → validate → transaction (insert + update) → unlock"
- "Snipe protection: if you bid in the last 30 seconds, the auction extends by 30s"
- "The reaper is a background daemon that runs every 1 second, like a heartbeat for auctions"
- "ImageStore handles all image processing: resize, JPEG encoding, thumbnail generation"

**Possible Instructor Questions:**
- *What are the 6 bid validations?* "Active status, not seller, not current winner, price matches expected, minimum 5% increment, not expired."
- *How does stale data detection work?* "The client sends the price it thinks is current. If someone else bid first, the server rejects with StaleDataException. The client then refreshes."
- *What is optimistic locking?* "Instead of locking the database row for reads, we detect conflicts at write time. If the data changed since we last read, we reject the write."
- *How does the reaper recover from a crash?* "At startup, we call recoverFromCrash() which runs sweepOverdue() — this finds all ACTIVE auctions past their endTime and transitions them."
- *Why per-auction locks instead of a global lock?* "Higher concurrency. 100 independent auctions can process 100 bids simultaneously."
- *How does image validation work?* "Server checks size ≤ 2MB, format is JPG/JPEG/PNG, dimensions ≤ 2000×2000. Re-encodes to JPEG to strip EXIF metadata."

**Simplify If Asked:**
- "Think of LockManager like a key for each auction. You need the key to change the auction. Two people can work on different auctions at the same time."
- "Snipe protection solves the eBay problem where someone bids at the last second and no one can respond. We give people 30 more seconds to counter-bid."

### 12.3 Betty — Client & JavaFX UI

**Key Talking Points:**
- "8 screens managed by 8 controllers, using FXML for layout and CSS for styling"
- "AtlantaFX PrimerDark theme gives it a modern, professional look"
- "PollingService provides real-time updates — gallery every 2s, detail every 1s"
- "Optimistic UI: when you bid, the price updates immediately — if it fails, we roll back"
- "LQIP pattern: small blurred placeholder first, full image loads in background"

**Possible Instructor Questions:**
- *How do you avoid freezing the UI?* "All RMI calls that could be slow are done on background threads via CompletableFuture.supplyAsync() or JavaFX Task. UI updates use Platform.runLater()."
- *What is optimistic UI?* "We update the UI immediately when the user clicks 'Bid', then confirm with the server. If the server rejects, we roll back. It feels instant."
- *How does polling work?* "ScheduledExecutorService runs every N seconds. It calls the RMI method, then passes the result to Platform.runLater() for UI updates."
- *What happens if the connection drops?* "After 3 failed polls, PollingService calls the failure callback. AuctionDetailController shows a 'Connection lost' banner. User can go back to Connect screen."
- *Explain the countdown timer.* "We sync the server clock once (serverTime() - clientTime = offset), then use JavaFX Timeline to update every second on the FX thread. Colors change from gray → yellow → red."
- *How do you handle window focus?* "We pause polling when the window loses focus and resume when it regains focus — saves unnecessary network calls."

**Simplify If Asked:**
- "Polling is like refreshing a web page every 2 seconds automatically"
- "Optimistic UI is like when you like a post on Instagram — it turns red immediately, even before the server confirms"

### 12.4 Bemigbar — Networking, Database, Concurrency & Testing

**Key Talking Points:**
- **Networking:** "Two protocols — UDP for discovery (no connection needed, just shout), RMI for everything else (login, bid, browse)"
- **Database:** "SQLite with INTEGER cents (no floating-point money!), parameterized queries, VACUUM INTO for backup"
- **Concurrency:** "Per-auction ReentrantLock + database transactions = thread-safe bidding"
- **Testing:** "21 test files including unit, integration, and stress tests with 10 concurrent threads"

**Possible Instructor Questions:**
- *How does UDP discovery work?* "Server broadcasts 'RTDAS|MainServer|1099' on port 9999 every 2 seconds. Client listens on the same port. No connection needed."
- *Why UDP instead of TCP?* "Discovery is inherently a broadcast problem. UDP supports broadcast addressing. If it fails (firewall), user can type IP manually."
- *How does RMI make an object remote?* "The interface extends Remote, the implementation extends UnicastRemoteObject, and we bind it in the RMI registry."
- *Why INTEGER cents?* "Floating-point can't represent money exactly. 0.1 + 0.2 = 0.30000000000000004 in double. Integers are exact."
- *How do you prevent SQL injection?* "Parameterized prepared statements everywhere — never concatenate user input into SQL strings."
- *Explain the transaction in placeBid.* "We set autoCommit=false, then insert the bid, update the auction price, and possibly extend the end time. If any step fails, everything rolls back. No partial updates."
- *What stress tests did you run?* "10 threads placing 20 bids each on the same auction simultaneously. The test verifies the final price was the maximum of all bid attempts."
- *How does the backup work without locking the database?* "SQLite's VACUUM INTO creates a clean copy atomically. It's a built-in SQLite feature."
- *Explain the overhead of thread-safe collections.* "CopyOnWriteArrayList for server discovery (few writes, many reads). ConcurrentHashMap for locks (thread-safe, non-blocking reads)."

**Simplify If Asked:**
- "RMI lets us call methods on another JVM like they're local — Java handles all the network plumbing"
- "UDP discovery is like shouting in a room to see who's there. RMI is like having a private conversation."
- "Stress test proves our locking works — 10 people bidding at once doesn't break anything."

---

## 13. Code Quality & Engineering Analysis

### 13.1 Good Design Decisions

| Decision | Why It's Good |
|----------|---------------|
| **Deep Module Architecture** | Business logic is independent of networking. Can test AuctionManager without RMI. |
| **Per-auction locking** | Higher concurrency than a global lock. Independent auctions don't block each other. |
| **INTEGER cents** | Avoids floating-point rounding errors in financial calculations. |
| **Parameterized queries** | Prevents SQL injection. All repositories use prepared statements. |
| **Optimistic bidding (client)** | Instant UI feedback. Rollback on failure. Better UX than waiting for server. |
| **AsyncLogger (BlockingQueue)** | Non-blocking logging. Server threads don't wait for disk I/O. |
| **Double-check in LifecycleManager** | Prevents race conditions between concurrent sweeps. |
| **Constants class** | Single source of truth for all magic values. Easy to find and change. |
| **Domain exceptions** | Each failure mode has its own exception type. Client shows specific error messages. |
| **FXML + CSS separation** | UI layout separate from logic. Easy to redesign without touching Java code. |

### 13.2 Weaknesses

| Weakness | Impact | How to Fix |
|----------|--------|------------|
| **Single SQLite connection** | No concurrent writes. Not scalable. | Connection pool (HikariCP) for production. |
| **SHA-256 without salt** | Dictionary attacks are feasible. | Use bcrypt/Argon2. |
| **No RMI callbacks (polling)** | Wasted network calls. Not truly real-time. | WebSockets or Server-Sent Events for production. |
| **No pagination** | Gallery loads ALL auctions every time. Won't scale beyond ~1000 items. | Add `LIMIT/OFFSET` to queries. |
| **Client-side filtering/sorting** | Loads all auctions, filters locally. Wastes bandwidth. | Server-side filtering (already partially implemented). |
| **No input trimming** | "  " counts as valid username/title. | Add validation in controllers. |
| **Hard-coded broadcast address** | `255.255.255.255` may not work on all networks. | Use network-specific broadcast addresses. |
| **Single admin panel CSS** | Loads `admin-panel.css` by filename match in ViewLoader. Fragile. | Explicit stylesheet assignment. |
| **Thread names hard-coded** | Strings like `auto-launch-watcher` are not constants. | Use constants. |
| **No @Override annotation in some methods** | Makes code harder to audit for interface changes. | Add missing annotations. |

### 13.3 Inconsistencies

| Inconsistency | Location | Description |
|---------------|----------|-------------|
| `Constants.IMAGES_DIR` = `"resources/images"` but `ImageStore` uses `"data/images"` | Constants.java vs ImageStore.java | Two different image directory constants. `ImageStore` uses its own hard-coded paths. |
| `AdminManager.createUser()` logs with `CREATE_AUCTION` event type | `AdminManager.java:48` | Should probably be `CREATE_USER` event type. |
| `searchActiveAuctions` default method in `IAuctionService` duplicates server logic | `IAuctionService.java` | The interface has a default implementation that filters client-side. Server has its own SQL-based implementation. |
| Audit log file path referenced in both `Constants` and `AsyncLogger` | Multiple files | `Constants.AUDIT_LOG_PATH` vs `AsyncLogger` using `logs/` directory + category file names |
| BidHistoryService uses `ForkJoinPool.commonPool()` | `BidHistoryService.java` | Unlike other services that use dedicated thread pools |

### 13.4 Possible Improvements

| Improvement | Effort | Impact |
|-------------|--------|--------|
| Add pagination to gallery queries | Medium | Prevents loading entire DB on every poll |
| Server-side filtering + sorting | Low | Eliminates client-side filter duplication |
| Replace polling with WebSockets or SSE | High | True real-time updates |
| Add password salt (bcrypt) | Low | Proper security |
| Add connection pool | Medium | Better concurrent DB access |
| Add monitoring (JMX or simple metrics) | Medium | Observability during demo |
| Add end-to-end encryption | High | Secure network communication |
| Internationalize UI messages | Medium | Wider audience |
| Add email notifications for outbid/won | Medium | Better user experience |
| Move to multi-module Maven | High | Cleaner separation of concerns |

### 13.5 Scalability Considerations

| Bottleneck | Why | Current Limit | Improvement |
|------------|-----|---------------|-------------|
| Single-threaded SQLite writes | SQLite is single-writer | ~50 writes/second | PostgreSQL or MySQL |
| Polling overhead | Every client polls every N seconds | ~100 clients | Server push (WebSockets) |
| In-memory session storage | Sessions in ConcurrentHashMap | ~10,000 sessions | Redis or database-backed storage |
| No image CDN | All images served through RMI | ~1000 images | Direct file serving or CDN |
| Single JVM | Server runs on one machine | CPU/RAM bound | Horizontal scaling with shared DB |

---

*Document generated for academic presentation purposes. Based on the actual implementation of RTDAS v1.0.0.*
