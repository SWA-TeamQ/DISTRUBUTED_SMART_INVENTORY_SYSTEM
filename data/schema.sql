-- RTDAS Schema
CREATE TABLE IF NOT EXISTS users (
    username TEXT PRIMARY KEY,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL CHECK(role IN ('ADMIN','USER')),
    created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS auction_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    description TEXT,
    category TEXT NOT NULL,
    starting_price_cents INTEGER NOT NULL CHECK(starting_price_cents >= 0),
    current_bid_cents INTEGER NOT NULL CHECK(current_bid_cents >= 0),
    highest_bidder_username TEXT,
    seller_username TEXT NOT NULL,
    start_time TEXT NOT NULL,
    end_time TEXT NOT NULL,
    cap_end_time TEXT NOT NULL,
    status TEXT NOT NULL CHECK(status IN ('ACTIVE','SOLD','EXPIRED','CANCELLED')),
    img1 TEXT, img2 TEXT, img3 TEXT,
    relisted_from INTEGER,
    FOREIGN KEY (seller_username) REFERENCES users(username),
    FOREIGN KEY (relisted_from) REFERENCES auction_items(id)
);

CREATE TABLE IF NOT EXISTS bids (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    auction_item_id INTEGER NOT NULL,
    bidder_username TEXT NOT NULL,
    amount_cents INTEGER NOT NULL CHECK(amount_cents > 0),
    timestamp TEXT NOT NULL,
    FOREIGN KEY (auction_item_id) REFERENCES auction_items(id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_username) REFERENCES users(username)
);

CREATE INDEX IF NOT EXISTS idx_bids_auction_id ON bids(auction_item_id);
CREATE INDEX IF NOT EXISTS idx_auction_status_end ON auction_items(status, end_time);
CREATE INDEX IF NOT EXISTS idx_auction_seller ON auction_items(seller_username);
