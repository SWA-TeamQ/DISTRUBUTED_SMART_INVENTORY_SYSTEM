# 💾 Database & Files

This document details how data is persisted and secured in the Real-Time Distributed Auction System (RTDAS).

## 1. Database (SQLite)
A single file (`auction.db`) is managed via the **Repository Pattern**:
* **DatabaseManager**: Handles the connection and schema initialization.
* **UserRepository**: Handles CRUD operations for users.
* **AuctionRepository**: Handles CRUD operations for auctions.
* **BidRepository**: Handles CRUD operations for bids.

SQLite is fast, needs no setup, and is perfect for a portable demo.

## 2. CSV Export
Sellers can export a spreadsheet of all their auctions. The file contains:
```text
AuctionID, Title, Category, StartingPrice, FinalPrice, Winner, Status, StartTime, EndTime
```
- `FinalPrice` = the closing price (or starting price if unsold)
- `Winner` = winner’s username (or empty)
- The server generates the CSV and sends it to the client; the client saves it via a “Save As…” dialog.

## 3. Audit Log
Every important action (login, bid, auction creation, cancellation, etc.) is recorded in `logs/audit.log` with a timestamp. Example:
```text
2026-05-06T14:30:00Z INFO bidder1: Placed bid 150.0 on auction 5
```
This gives the admin a tamper‑resistant history of everything that happened.

## 4. Security
* **SHA‑256 Hashing:** We never store your real password. Only a long, unique hash (like a fingerprint) is saved in the database. Even if someone steals the file, they can’t reverse‑engineer your password.
* **Admin‑Only Registration:** Normal users cannot create their own accounts. An admin creates usernames and passwords, keeping control over who accesses the system.
