# 🔨 Simple Guide to Our RMI Auction House

This document explains how our English Auction system works and defines the technical terms we are using in our project.

---

## 🏗️ 1. The Core Concept: "English Auction"
Think of this like **eBay**. 
1. An item starts at a low price.
2. People bid against each other.
3. The price goes **UP**.
4. The person with the highest bid when the clock hits zero wins.

---

## 📝 2. Key Terms (The "Lingo")

### **The Players**
* **Server:** The "Brain." It holds the database, tracks the timers, and decides who won.
* **Client (Bidder/Seller):** The app you run on your laptop to interact with the Server.
* **RMI (Remote Method Invocation):** The "Telephone Line." It allows the Client to call a function that actually runs on the Server.

### **The Auction Rules**
* **Minimum Increment:** You can't just outbid someone by $0.01. You must bid at least **5% more** than the current price. This keeps the auction moving.
* **Snipe Protection:** If someone bids in the last **30 seconds**, the timer resets back to 30 seconds. This prevents people from "stealing" the item at the very last millisecond.
* **Polling:** Since the Server doesn't "call" the Client, your app asks the server every 2 seconds: *"Hey, any new bids? What's the time left?"*

---

## ⚙️ 3. How the Code Logic Works

### **Concurrency (The "First-Come" Rule)**
Since multiple friends might click "Bid" at the exact same time, we use **Synchronization**. 
> **Scenario:** Alice and Bob both bid $100 at the same time. 
> **Result:** The Server processes whoever's "packet" arrived 1 millisecond earlier, and tells the other person: *"Too slow! The price is already $100."*

### **State Management**
Every item in the database has a **Status**:
1.  **ACTIVE:** Bidding is open.
2.  **SOLD:** Timer hit zero and someone bid.
3.  **EXPIRED:** Timer hit zero but *nobody* bid.

---

## 🔐 4. Security (Simple Version)
We don't store your "real" password. We use **SHA-256 Hashing**.
* If your password is `Coffee123`, we store a long string of random-looking gibberish: `a7f12...`
* Even if a hacker steals the database, they can't see your actual password.

---

## 🚀 5. Example Flow
1. **Seller** creates an auction for a "Vintage Laptop" starting at $50.
2. **Bidder A** bids $55.
3. **Bidder B** tries to bid $56 (Rejected! Must be 5% higher -> $57.75).
4. **Bidder B** bids $60.
5. **Timer** hits 0 seconds -> Server marks item as **SOLD** to Bidder B.