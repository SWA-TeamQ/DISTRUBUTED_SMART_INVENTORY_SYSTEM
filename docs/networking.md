# 🌐 Networking & Concurrency

This document outlines how the Real-Time Distributed Auction System (RTDAS) components communicate and stay thread-safe.

## 1. Connection & Discovery
We aren’t using the internet — just a local Wi‑Fi router. Two or more laptops connect directly.

* **Server Discovery (UDP Broadcast):** When the server starts, it sends a small “I’m here!” message every 2 seconds across the local network. All clients can see a list of available servers automatically.
* **Manual IP Entry:** If the auto‑discovery fails (some networks block broadcasts), you can type the server’s IP address manually in a text field.

## 2. RMI Polling vs Push
* **Client Polling:** The client does not receive instant push notifications. Instead, it politely asks the server every 2 seconds: *“Any updates? What’s the new highest bid? How much time is left?”* This is simple, reliable, and avoids complex firewall issues.
* **Stale Price Detection:** Your screen might show an old price because of network lag. When you place a bid, the client says: “I bid X, and I think the current price is Y”. The server checks: “Is it still Y?” If not, it rejects the bid and tells your app to refresh — no “ghost” bids.

## 3. Concurrency & Performance
* **Server‑side synchronized bidding:** When multiple people bid on the same item at exactly the same moment, the server uses a lock to process them one at a time. The first one that arrives wins; the others are politely rejected.
* **UI Thread Safety:** Every RMI call (bidding, loading images) runs on a background thread. The JavaFX interface never freezes, even during a large image download.
* **Client polling stops when not needed:** When you leave the auction detail view, the polling thread shuts down — no wasted resources.
