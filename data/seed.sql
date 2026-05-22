-- Note: All passwords are 'password' (or 'admin' for admin user)
-- Hashed using com.auction.server.util.SecurityUtil.hashPassword()

-- Admin
INSERT INTO users (username, password_hash, role, created_at) VALUES ('admin', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'ADMIN', '2026-05-21T00:00:00Z');

-- Users
INSERT INTO users (username, password_hash, role, created_at) VALUES ('alice', '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', 'USER', '2026-05-21T00:01:00Z');
INSERT INTO users (username, password_hash, role, created_at) VALUES ('bob', '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', 'USER', '2026-05-21T00:02:00Z');
INSERT INTO users (username, password_hash, role, created_at) VALUES ('charlie', '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', 'USER', '2026-05-21T00:03:00Z');

-- Auctions
INSERT INTO auction_items (title, description, category, starting_price_cents, current_bid_cents, seller_username, start_time, end_time, cap_end_time, status) 
VALUES ('Vintage Clock', '19th-century mechanical clock', 'HOME', 5000, 5000, 'alice', '2026-05-21T10:00:00Z', '2026-06-21T10:00:00Z', '2026-06-21T10:00:00Z', 'ACTIVE');

INSERT INTO auction_items (title, description, category, starting_price_cents, current_bid_cents, seller_username, start_time, end_time, cap_end_time, status) 
VALUES ('Antique Vase', 'Ming dynasty style', 'COLLECTIBLES', 15000, 15000, 'alice', '2026-05-21T10:00:00Z', '2026-06-21T10:00:00Z', '2026-06-21T10:00:00Z', 'ACTIVE');

INSERT INTO auction_items (title, description, category, starting_price_cents, current_bid_cents, seller_username, start_time, end_time, cap_end_time, status) 
VALUES ('Modern Laptop', 'High-performance work laptop', 'ELECTRONICS', 80000, 80000, 'bob', '2026-05-21T10:00:00Z', '2026-06-21T10:00:00Z', '2026-06-21T10:00:00Z', 'ACTIVE');

INSERT INTO auction_items (title, description, category, starting_price_cents, current_bid_cents, seller_username, start_time, end_time, cap_end_time, status) 
VALUES ('Leather Sofa', 'Comfortable 3-seater', 'FURNITURE', 30000, 30000, 'charlie', '2026-05-21T10:00:00Z', '2026-06-21T10:00:00Z', '2026-06-21T10:00:00Z', 'ACTIVE');
