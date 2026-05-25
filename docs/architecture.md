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

## 2. Server Architecture: Deep Modules

The RTDAS server is built using a **Deep Module** architecture to ensure high **leverage** and **locality**.

| Module | Interface (Seam) | Implementation (Depth) |
|--------|------------------|------------------------|
| **AuctionManager** | `placeBid`, `createAuction` | All bidding invariants (5%, snipe, stale), transaction safety |
| **LifecycleManager** | `sweepOverdue` | State machine (ACTIVE -> SOLD/EXPIRED), termination logic |
| **ImageStore** | `saveAuctionImages` | Filesystem I/O + Database path synchronization |
| **AuctionServiceImpl** | `IAuctionService` (RMI) | Thin **Adapter**; handles networking and session auth only |

### Why This Design?

- **Locality**: Bidding rules (e.g., "5% increment") live only in `AuctionManager`. Deleting RMI doesn't lose this logic.
- **Leverage**: The RMI service is a simple pass-through. It is "shallow" by design, delegating complex behavior to "deep" managers.
- **Testability**: Managers can be unit-tested without starting RMI or mocking network exceptions.

---

## 3. Component Interactions

### Bidding Flow

1. **Client** calls `placeBid` over RMI.
2. **AuctionServiceImpl** (Adapter) validates session token.
3. **AuctionManager** (Core) enforces all domain rules:
    - Minimum increment (5%)
    - Self-bid prevention
    - Stale data detection (optimistic locking)
    - Snipe protection (extends timer)
4. **AuctionRepository** & **BidRepository** persist changes.

### Expiration Flow (The Reaper)

1. **AuctionReaper** (Trigger) runs every 1 second.
2. It calls `LifecycleManager.sweepOverdue()`.
3. **LifecycleManager** identifies overdue auctions and executes state transitions.
4. Transitions are logged to the database and audit trail.

---

## 4. Key Mechanics

### Minimum Increment (5% Rule)

Enforced by `AuctionManager`:
```java
if (amount < currentBid * 1.05) throw new InsufficientBidException();
```

### Snipe Protection with Cap

Triggered by `AuctionManager` during bid placement if `now` is within 30s of `endTime`. 
- Effect: `endTime = min(endTime + 30s, capEndTime)`
- `capEndTime` is set at auction creation to prevent auctions from running indefinitely.

### Clock Authority (Server-Time)

To prevent discrepancies between client and server clocks during countdowns, the server provides a `serverTime()` method. Clients compute a drift offset on connection and use it to adjust their local timers to match the server's authoritative clock.

---

## 5. RMI Interface Contract (IAuctionService)

Full contract in `shared/interfaces/IAuctionService.java`.

### Key Methods

| Method | Role | Delegation |
|--------|------|------------|
| `placeBid(...)` | Adapter | `AuctionManager.placeBid(...)` |
| `createAuction(...)` | Adapter | `AuctionManager.createAuction(...)` + `ImageStore` |
| `getActiveAuctions()` | Adapter | `AuctionManager.getActiveAuctions()` |
| `serverTime()` | Adapter | Authority for synchronization |

---

## 6. Concurrency Model

### Server-Side Locking

`AuctionManager` manages per-auction concurrency using a `ConcurrentHashMap` of `ReentrantLock` objects. Every mutation (bid placement, cancellation, relisting) must acquire the lock for the specific auction ID.

### Atomic Bid Commit

Bid placement is executed within a single database transaction:
1. Validate rules (increment, self-bid, etc.)
2. Insert bid record.
3. Update auction record (new price, highest bidder, potentially extended end time).
4. Commit or rollback on failure.

### Background Lifecycle

`AuctionReaper` runs as a daemon thread, ensuring terminal states are reached even if no users are active. It also acquires the auction-specific lock before performing transitions.

---

## 7. Out of Scope

- RMI callbacks (clients must poll).
- Salted password hashing (using SHA-256 for demo simplicity).
- Horizontal scaling.
- Audit log tamper-resistance (simple append-only text file).