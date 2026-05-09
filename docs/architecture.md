# 🏗️ RTDAS Architecture & Design Decisions

System architecture, component interactions, and rationale for key technical choices.

---

## 1. The Core Concept: English Auction

An English auction is an open-outcry ascending auction where:

1. An item starts at a low price (starting price)
2. Bidders compete by placing successively higher bids
3. Each new bid must meet a minimum increment (5%)
4. When the timer expires, the highest bidder wins

### Why This Model?

- **Natural concurrency demo:** Multiple bidders racing creates natural race conditions
- **Simplicity:** Easy to explain, implement, and test
- **Real-world analogy:** Familiar to users (eBay-style)

---

## 2. Key Terms & Components

| Term | Description |
|------|-------------|
| **Server** | Headless RMI service; owns SQLite DB, business logic |
| **Client** | JavaFX desktop app; three roles (Admin/Seller/Bidder) |
| **RMI** | Primary communication channel; client polls server |
| **UDP Broadcast** | Server discovery; server broadcasts every 2s on port 9999 |
| **Admin** | Super-user; creates accounts, backups, views logs |
| **Seller** | Can create/cancel/relist auctions, export CSV |
| **Bidder** | Can browse and bid on others' auctions |

---

## 3. Auction Rules & Mechanics

### Minimum Increment (5% Rule)

```
if bids_exist:
    min_bid = current_bid * 1.05
else:
    min_bid = starting_price
```

**Rationale:** Prevents penny-sniping while keeping mathematics simple.

### Snipe Protection

- **Trigger:** Bid placed when `endTime - now < 30s`
- **Effect:** `endTime = min(endTime + 30s, capEndTime)`
- **Cap:** `capEndTime = originalEndTime + 10 minutes`

**Rationale:** Discourages last-second steals without allowing infinite extension wars.

### Self-Bid Prevention

Server validates `bidderUsername != sellerUsername` before accepting bid.

### Duplicate Bid Prevention

Server tracks `highestBidderUsername`; rejects bid if already winning.

---

## 4. Auction Lifecycle State Machine

```
                    ┌─────────────┐
                    │   ACTIVE     │
                    └──────┬───────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
       (has bids)     (no bids)   (seller cancels, 0 bids)
              │            │            │
              ▼            ▼            ▼
        ┌────────┐   ┌────────┐   ┌──────────┐
        │  SOLD  │   │EXPIRED│   │CANCELLED │
        └────────┘   └───┬────┘   └──────────┘
                         │
                    (relist via)
                    ┌────────┐
                    │ ACTIVE │
                    └────────┘
```

| State | Entry Condition | Exit Condition |
|-------|-----------------|----------------|
| ACTIVE | Created by seller | Timer expires OR seller cancels |
| SOLD | Timer expired + bids exist | None (terminal) |
| EXPIRED | Timer expired + no bids | Relisted (creates new ACTIVE) |
| CANCELLED | Seller action | None (terminal) |

---

## 5. The Auction Reaper

A background `ScheduledExecutorService` running every 1 second.

### Responsibilities

1. Query for `status='ACTIVE' AND end_time < NOW()`
2. Acquire per-auction `ReentrantLock`
3. If bids exist → transition to `SOLD`
4. If no bids → transition to `EXPIRED`
5. On server startup: sweep any overdue auctions from crash recovery

### Why a Background Thread?

- **Decouples time from user action:** Auction closes automatically
- **Demonstrates multithreading:** Independent from request threads
- **Prevents missed closures:** Even if no clients are connected

---

## 6. RMI Interface Contract (IAuctionService)

Full contract defined in `shared/interfaces/IAuctionService.java`.

### Key Methods (selected)

| Method | Return | Auth | Purpose |
|--------|--------|------|---------|
| `login(String, String)` | `Session` | None | Returns token for session |
| `logout(String token)` | `void` | Token | Invalidates session |
| `serverTime()` | `String` | None | UTC ISO-8601 for clock sync |
| `getActiveAuctions()` | `List<AuctionItem>` | Token | Gallery view |
| `getAuctionById(int)` | `AuctionItem` | Token | Detail view |
| `placeBid(int, token, long cents, long expected)` | `void` | Token | Bid with stale detection |
| `createAuction(AuctionItem, byte[]...)` | `int` | Token | Seller creates |
| `cancelAuction(int, token)` | `void` | Token | Seller cancels |
| `relistAuction(int, end, token)` | `int` | Token | Creates new auction from expired |
| `getMyBids(token)` | `List<Bid>` | Token | Bidder activity |
| `getMyWonAuctions(token)` | `List<AuctionItem>` | Token | Bidder activity |
| `exportAuctionsToCSV(token)` | `byte[]` | Token | Seller dashboard |
| `createUser(adminToken, ...)` | `void` | Admin token | Admin creates user |
| `backupDatabase(adminToken)` | `byte[]` | Admin token | Admin backup |
| `getAuditLogs(adminToken, N)` | `List<String>` | Admin token | Admin monitoring |

### Why Sessions?

**Decision:** Every mutating call requires a session token issued by `login()`.

**Rationale:** Without tokens, any client can impersonate any user by passing their username. Tokens provide:
- Proof of authentication
- Ability to revoke sessions (logout)
- Path to implement rate-limiting per session

---

## 7. Concurrency Model

### Server-Side Bidding Flow

```java
// Per-auction lock map
ConcurrentHashMap<Integer, ReentrantLock> locks = new ConcurrentHashMap<>();

public void placeBid(int auctionId, String token, long cents, long expected) {
    User user = sessionManager.validate(token);
    AuctionItem item = getAuctionById(auctionId);
    
    ReentrantLock lock = locks.computeIfAbsent(auctionId, k -> new ReentrantLock());
    lock.lock();
    try {
        // All validations inside lock
        validateFreshness(item.getCurrentBidCents(), expected);
        validateNotSelfBid(item, user);
        validateNotAlreadyWinning(item, user);
        validateMinimumIncrement(item, cents);
        validateActiveAndNotExpired(item);
        
        // Atomic update in single transaction
        updateBidAndAuction(item, user, cents, lock);
    } finally {
        lock.unlock();
    }
}
```

### Why Per-Auction Locks?

- **Fine-grained concurrency:** Bids on different auctions don't block each other
- **Simple reasoning:** Each lock protects exactly one auction's state
- **Small footprint:** Only active auctions have locks in the map

### Client-Side Concurrency

- **All RMI calls in `javafx.concurrent.Task`** → UI never freezes
- **Polling via `ScheduledExecutorService`** every 2s
- **Results delivered via `Platform.runLater()`**
- **Thread cleanup:** Polling stops when view is closed (`shutdown()` method)

---

## 8. Clock Synchronization

**Problem:** Client clocks may drift from server clock, causing:
- Countdown inaccuracies
- Wrong "snipe" detection (if `now` is from client)

**Solution:**
1. Client calls `serverTime()` on connect, stores offset
2. All countdowns use `offset + clientNow = serverNow`
3. This is **not a strict NTP**; acceptable for a LAN demo

---

## 9. Error Handling Philosophy

| Error | Client Behavior | Server Behavior |
|-------|-----------------|-----------------|
| Bad token | Alert + redirect to Connect | `UnauthorizedException` |
| Stale price | Shake input, show new price | `StaleDataException` |
| Self-bid | Alert explaining | `SelfBidException` |
| Already winning | Alert | `DuplicateBidException` |
| Rate limited | Alert with "try again in Xs" | `RateLimitedException` |
| Connection lost | "Connection lost" banner, retry every 5s | N/A (client-side) |

---

## 10. Out of Scope (Architecture)

- RMI callbacks/push notifications
- Internet deployment / NAT traversal
- JWT or OAuth; simple UUID tokens
- Salted password hashing; SHA-256 only
- Horizontal scaling; single-server design