# 🏗️ Architecture & Core Mechanics

This document explains the core architecture of the Real-Time Distributed Auction System (RTDAS).

## 1. The Core Concept: "English Auction"
Imagine a live eBay sale:
1. An item starts at a low price.
2. People keep outbidding each other.
3. The price only goes **UP**.
4. When the countdown hits zero, the highest bidder wins and pays their bid.

## 2. Key Terms (The "Lingo")

### 👥 The Players
* **Server:** The brain. It stores all data, runs the clock, enforces rules, and decides who wins.
* **Client:** The app on your laptop. It gives you a graphical interface (JavaFX) and talks to the server via RMI.
* **RMI (Remote Method Invocation):** The telephone line. A technology that lets a client call a method that actually runs on the server, as if it were local.
* **Admin:** The super-user who creates accounts, backs up the database, and views system logs.
* **Seller:** A user who can create auctions, manage their own listings, export records, and also bid on other people’s items.
* **Bidder:** A user who browses active auctions and places bids.

### 🏛️ Auction Rules & Mechanics
* **Minimum Increment (5% rule):** You cannot outbid someone by 1 cent. Every new bid must be at least **5% higher** than the current price. This keeps the game fair and stops penny-sniping.
* **Snipe Protection:** If a bid is placed within the last **30 seconds** of an auction, the countdown timer **resets to 30 seconds**. This gives everyone a chance to react and prevents last‑millisecond “steals”.
* **Self-Bid Prevention:** A seller cannot bid on their own auction. The server blocks it.
* **Duplicate Bid Prevention:** You cannot place a bid if you are already the highest bidder. The server tells you you’re already winning.

### 🔄 Auction Lifecycle (The State Machine)
Every auction follows a strict path:
```text
ACTIVE ──── (timer expires, has bids) ──→ SOLD
ACTIVE ──── (timer expires, no bids) ──→ EXPIRED
ACTIVE ──── (seller cancels, zero bids) → CANCELLED
EXPIRED ─── (seller relists) ──────────→ ACTIVE
```
* **ACTIVE:** Bidding is open.
* **SOLD:** Timer hit zero and at least one bid was placed. Winner gets the item.
* **EXPIRED:** Timer hit zero but nobody placed a bid. The seller can relist it.
* **CANCELLED:** The seller manually removed the item before it ended (only allowed if zero bids).
* **Relisting:** If an auction expired with no bids, the seller can turn it back to ACTIVE with a new end time.

### 🧹 The Auction Reaper
A background thread that runs **every second** on the server. It looks for any auction whose `end_time` has passed and automatically transitions it to **SOLD** (if there were bids) or **EXPIRED** (if no bids). It keeps the system moving without human intervention.
