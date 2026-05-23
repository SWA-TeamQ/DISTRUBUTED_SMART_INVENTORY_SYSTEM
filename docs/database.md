# 💾 RTDAS Database Specification

SQLite schema, repository architecture, backup strategy, and security measures.

---

## 1. Database Overview

- **Engine:** SQLite (single file: `data/auction.db.sqlite`)
- **Connection:** Single shared `Connection` in `DatabaseManager`
- **Pattern:** Repository pattern with dedicated classes per table

| Repository | File |
|------------|------|
| `UserRepository` | `repository/UserRepository.java` |
| `AuctionRepository` | `repository/AuctionRepository.java` |
| `BidRepository` | `repository/BidRepository.java` |

---

## 2. Schema Definition

### users Table

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `username` | `TEXT` | `PRIMARY KEY` | Unique login name |
| `password_hash` | `TEXT` | `NOT NULL` | SHA-256 hex string |
| `role` | `TEXT` | `NOT NULL` | `ADMIN`, `SELLER`, or `BIDDER` |
| `created_at` | `TEXT` | `NOT NULL` | ISO-8601 UTC |

### auction_items Table

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | |
| `title` | `TEXT` | `NOT NULL` | |
| `description` | `TEXT` | | Optional, may be empty |
| `category` | `TEXT` | `NOT NULL` | Enum: ELECTRONICS, FURNITURE, ART, OTHER |
| `starting_price_cents` | `INTEGER` | `NOT NULL CHECK >= 0` | |
| `current_bid_cents` | `INTEGER` | `NOT NULL CHECK >= 0` | |
| `highest_bidder_username` | `TEXT` | | NULL if no bids |
| `seller_username` | `TEXT` | `NOT NULL` | FK to users.username |
| `start_time` | `TEXT` | `NOT NULL` | ISO-8601 UTC |
| `end_time` | `TEXT` | `NOT NULL` | ISO-8601 UTC |
| `cap_end_time` | `TEXT` | `NOT NULL` | Snipe limit (end_time + 10 min) |
| `status` | `TEXT` | `NOT NULL CHECK IN (...)` | ACTIVE, SOLD, EXPIRED, CANCELLED |
| `img1` | `TEXT` | | Filename or NULL |
| `img2` | `TEXT` | | Filename or NULL |
| `img3` | `TEXT` | | Filename or NULL |
| `relisted_from` | `INTEGER` | | FK to auction_items.id (NULL if not a relist) |

### bids Table

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | |
| `auction_id` | `INTEGER` | `NOT NULL REFERENCES auction_items(id) ON DELETE CASCADE` | |
| `bidder_username` | `TEXT` | `NOT NULL` | FK to users.username |
| `amount_cents` | `INTEGER` | `NOT NULL CHECK > 0` | |
| `timestamp` | `TEXT` | `NOT NULL` | ISO-8601 UTC |

---

## 3. Indexes

```sql
CREATE INDEX idx_bids_auction_id ON bids(auction_id);
CREATE INDEX idx_auction_status_end ON auction_items(status, end_time);
CREATE INDEX idx_auction_seller ON auction_items(seller_username);
```

**Rationale:**
- `idx_bids_auction_id`: Fast lookup for bid history
- `idx_auction_status_end`: Reaper query `status='ACTIVE' AND end_time < now`
- `idx_auction_seller`: Seller dashboard queries

---

## 4. Data Types: Why Cents?

All monetary values are stored as `INTEGER` cents, not `DOUBLE` dollars.

| Aspect | Double (dollars) | Integer (cents) |
|--------|------------------|-----------------|
| Equality checks | Brittle (`0.1 + 0.2 != 0.3`) | Exact (`10 + 20 == 30`) |
| SQL constraints | Floating-point imprecision | Exact integer math |
| Client display | Format on conversion | Format on conversion |

---

## 5. Transactions

**All bid commits are atomic:**

```java
connection.setAutoCommit(false);
try {
    // 1. Insert new bid
    // 2. Update current_bid_cents, highest_bidder_username
    // 3. Extend end_time if snipe (inside cap)
    connection.commit();
} catch (SQLException e) {
    connection.rollback();
    throw e;
}
```

**Why?** Prevents inconsistent state if server crashes mid-bid.

---

## 6. Backup Strategy

### Online Backup API

```java
// SQLite VACUUM INTO is atomic and non-blocking
String backupPath = "data/backup-" + timestamp + ".db";
try (Statement stmt = connection.createStatement()) {
    stmt.execute("VACUUM INTO '" + backupPath + "'");
}
// Read file as bytes, return over RMI
```

| Feature | Implementation |
|---------|----------------|
| Trigger | Admin clicks "Backup" |
| Location | `data/auction_backup_YYYYMMDD.db` |
| Format | Full `.db` file (binary) |
| Transfer | Returned as `byte[]` over RMI, saved via `FileChooser` |

---

## 7. CSV Export

### Columns

| Column | Source | Notes |
|--------|--------|-------|
| AuctionID | `id` | |
| Title | `title` | |
| Category | `category` | |
| StartingPrice | `starting_price_cents / 100` | Formatted |
| FinalPrice | `current_bid_cents / 100` | Same as starting if no bids |
| Winner | `highest_bidder_username` | Empty string if none |
| Status | `status` | ACTIVE, SOLD, EXPIRED, CANCELLED |
| StartTime | `start_time` | ISO-8601 |
| EndTime | `end_time` | ISO-8601 |

### Rules

- Only auctions where `seller_username = ?`
- All statuses included (not just active)
- RFC 4180 escaping for commas, quotes, newlines in title/description

---

## 8. Security

| Aspect | Implementation |
|--------|----------------|
| Passwords | SHA-256 hash only (no salt) — acceptable for university demo |
| Registration | Admin-only via `createUser()`; no public endpoint |
| SQL Injection | Parameterized queries throughout |

---

## 9. Initialization

On first run:

```java
// Enable FK support
connection.createStatement().execute("PRAGMA foreign_keys = ON");

// Create tables if not exist (see schema above)
// Insert default admin if users table empty
// Create directories: data/, logs/, resources/images/, resources/thumbs/
```

---

## 10. Audit Log

| Aspect | Detail |
|--------|--------|
| Path | `logs/audit.log` |
| Format | `[ISO-8601] [LEVEL] actor: description` |
| Rotation | None (append-only, manual delete) |
| Examples | See architecture.md examples |

**Note:** The term "tamper-resistant" has been removed from documentation; it is a simple append-only text file.