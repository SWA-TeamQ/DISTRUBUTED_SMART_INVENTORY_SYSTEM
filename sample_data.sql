BEGIN TRANSACTION;

-- Users (simple hashes for local testing)
INSERT OR IGNORE INTO users (username, password_hash, role, created_at) VALUES
('seller2','5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8','USER','2026-05-01T00:00:00Z'),
('seller3','5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8','USER','2026-05-01T00:00:00Z'),
('seller4','5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8','USER','2026-05-01T00:00:00Z'),
('seller5','5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8','USER','2026-05-01T00:00:00Z'),
('bidder11','5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8','USER','2026-05-10T00:00:00Z'),
('bidder12','5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8','USER','2026-05-10T00:00:00Z'),
('bidder21','5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8','USER','2026-05-15T00:00:00Z'),
('bidder31','5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8','USER','2026-05-20T00:00:00Z');

-- Auction items with `category` using enum names (ELECTRONICS, FURNITURE, ART, OTHER)
INSERT OR REPLACE INTO auction_items (id, title, description, category, starting_price_cents, current_bid_cents, highest_bidder_username, seller_username, start_time, end_time, cap_end_time, status, img1, img2, img3, relisted_from) VALUES
(101, 'Vintage Leica III', 'Classic rangefinder, needs CLA', 'ELECTRONICS', 50000, 65000, 'bidder11', 'seller2', '2026-05-01T09:00:00Z', '2026-06-01T12:00:00Z', '2026-06-01T12:00:00Z', 'ACTIVE', NULL, NULL, NULL, NULL),
(102, 'Handmade Patchwork Quilt', 'Queen size, hand-stitched', 'FURNITURE', 20000, 0, NULL, 'seller3', '2026-06-10T10:00:00Z', '2026-06-17T18:00:00Z', '2026-06-17T18:10:00Z', 'SCHEDULED', NULL, NULL, NULL, NULL),
(103, 'Signed First Edition', 'Hardcover, excellent', 'ART', 120000, 150000, 'bidder21', 'seller4', '2026-05-20T08:00:00Z', '2026-05-30T20:00:00Z', '2026-05-30T20:00:00Z', 'ACTIVE', NULL, NULL, NULL, NULL),
(104, 'Antique Oak Side Table', 'Restored, small blemish', 'FURNITURE', 30000, 45000, 'bidder31', 'seller5', '2026-04-01T08:00:00Z', '2026-04-07T20:00:00Z', '2026-04-07T20:00:00Z', 'SOLD', NULL, NULL, NULL, NULL),
(105, 'Limited Print Poster', 'Framed, 24x36', 'ART', 8000, 9500, 'bidder31', 'seller2', '2026-05-25T12:00:00Z', '2026-06-05T22:00:00Z', '2026-06-05T22:05:00Z', 'ACTIVE', NULL, NULL, NULL, NULL),
(106, 'Handcrafted Ceramic Vase', 'Artist-signed, small chip near base', 'OTHER', 6000, 7000, 'bidder12', 'seller3', '2026-06-01T09:00:00Z', '2026-06-08T17:00:00Z', '2026-06-08T17:00:00Z', 'SCHEDULED', NULL, NULL, NULL, NULL);

-- Recent bids (explicit ids)
INSERT OR REPLACE INTO bids (id, auction_item_id, bidder_username, amount_cents, timestamp) VALUES
(1001, 101, 'bidder11', 65000, '2026-05-26T14:02:10Z'),
(1002, 101, 'bidder12', 64000, '2026-05-26T13:55:05Z'),
(1003, 103, 'bidder21', 150000, '2026-05-25T09:10:30Z'),
(1004, 105, 'bidder31', 9500, '2026-05-26T10:22:00Z'),
(1005, 105, 'bidder12', 9200, '2026-05-26T09:50:12Z'),
(1006, 104, 'bidder31', 45000, '2026-04-07T20:00:00Z');

COMMIT;
