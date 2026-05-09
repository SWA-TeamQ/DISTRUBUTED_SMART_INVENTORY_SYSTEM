# Real-Time Distributed Auction System (RTDAS) Documentation

## Overview
RTDAS is a distributed, multi-user auction platform that enables real-time bidding using a client-server architecture. The solution showcases advanced Java topics: OOP, collections, multithreading, file I/O, JDBC, and Java RMI. The system includes a headless server for business logic and persistence, and a JavaFX desktop client for role-based interaction.

## Architecture Summary
### Layers
- **Presentation (JavaFX):** FXML views and controllers for user interactions.
- **Service (RMI Server):** Remote interface implementation, request handling, and synchronization.
- **Data Access (DAL):** JDBC for structured data and Java I/O for file streams (CSV, logs, binaries).

### Technology Stack
- **GUI:** JavaFX 17+ (`javafx.controls`, `javafx.fxml`)
- **Networking:** Java RMI (`java.rmi.*`, `java.rmi.server.*`)
- **Persistence:** SQLite or H2 (`java.sql.*`)
- **System I/O:** `java.io.*`, `java.nio.file.*`
- **Concurrency:** `java.util.concurrent.locks`

## Functional Requirements
### Role-Based Access Control (RBAC)
- **User (Abstract):** `username`, `password`, `roleType`
- **Bidder:** browse active auctions, `placeBid()`, view own bid history
- **Seller:** all Bidder permissions + `createAuction()`, manage own listings, `exportAuctionsToCSV()`
- **Admin:** all Seller permissions + `backupDatabase()`, `viewSystemLogs()`, `manageUsers()`

### Auction & Collections
- **AuctionItem Model:** `Serializable`, `Comparable<AuctionItem>`
- **Bid Model:** `Serializable` — records bidder, amount, timestamp
- **Server Cache:** `HashMap<Integer, AuctionItem>` for active auctions
- **Bid Ordering:** `PriorityQueue` for highest-bid tracking per auction
- **Client Sorting:** Comparators for end time, current bid, and category

### Hybrid Persistence
- **Database:** `auction_items` table (`ID`, `Title`, `Description`, `StartingPrice`, `CurrentBid`, `HighestBidder`, `SellerUsername`, `StartTime`, `EndTime`, `Active`)
- **Database:** `bids` table (`ID`, `AuctionItemId`, `BidderUsername`, `Amount`, `Timestamp`)
- **Database:** `users` table (`Username`, `Password`, `RoleType`)
- **CSV:** Buffered read/parse into `AuctionItem` objects
- **Logging:** Append-only audit log entries

## Distributed Interface (RMI)
Core contract between client and server:
- `List<AuctionItem> getActiveAuctions()`
- `AuctionItem getAuctionById(int auctionId)`
- `void placeBid(int auctionId, String bidderUsername, double amount)`
- `int createAuction(AuctionItem item)`
- `List<Bid> getBidHistory(int auctionId)`
- `byte[] exportAuctionsToCSV()`
- `void importAuctionsFromCSV(byte[] fileData)`
- `User login(String username, String password)`

## Non-Functional Requirements
### Server-Side Concurrency
- **Thread Safety:** `placeBid` guarded with `synchronized` or `ReentrantLock`
- **Scalability:** Stateless or thread-safe service design
- **Auction Timer:** Background thread to auto-close expired auctions

### Client-Side Concurrency
- **Non-Blocking UI:** All RMI calls in `javafx.concurrent.Task`
- **Live Bid Updates:** Polling thread refreshes auction state via `Platform.runLater()`
- **Countdown Timer:** Thread updates remaining time on active auctions

## Data Dictionary
### `auction_items` Table
- `id`: INTEGER, PK, AUTOINCREMENT  
- `title`: VARCHAR(100), NOT NULL  
- `description`: TEXT  
- `starting_price`: DOUBLE, CHECK (starting_price >= 0)  
- `current_bid`: DOUBLE, CHECK (current_bid >= 0)  
- `highest_bidder`: VARCHAR(50)  
- `seller_username`: VARCHAR(50), NOT NULL  
- `start_time`: DATETIME, NOT NULL  
- `end_time`: DATETIME, NOT NULL  
- `active`: BOOLEAN, DEFAULT TRUE

### `bids` Table
- `id`: INTEGER, PK, AUTOINCREMENT  
- `auction_item_id`: INTEGER, FK → `auction_items.id`  
- `bidder_username`: VARCHAR(50), NOT NULL  
- `amount`: DOUBLE, CHECK (amount > 0)  
- `timestamp`: DATETIME, NOT NULL

### File Layout
- `/data/auction_backup.db`
- `/logs/system_audit.log`
- `/exports/report_YYYYMMDD.csv`

## Error Handling
- **RemoteException:** Display JavaFX alert on connectivity failure
- **AuctionException:** Thrown if `placeBid` amount is below current bid, auction has ended, or bidder is the seller