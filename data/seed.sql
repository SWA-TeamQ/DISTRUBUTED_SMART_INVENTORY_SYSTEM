-- Seed data for RTDAS
INSERT INTO users (username, password_hash, role) VALUES ('admin', '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', 'ADMIN');
INSERT INTO users (username, password_hash, role) VALUES ('seller1', '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', 'USER');
INSERT INTO users (username, password_hash, role) VALUES ('bidder1', '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', 'USER');

INSERT INTO auction_items (title, description, category, starting_price, current_bid, seller_username, start_time, end_time, status) 
VALUES ('Vintage Clock', 'A beautiful 19th-century clock.', 'COLLECTIBLES', 50.0, 50.0, 'seller1', '2026-05-21T10:00:00Z', '2026-05-30T10:00:00Z', 'ACTIVE');

INSERT INTO auction_items (title, description, category, starting_price, current_bid, seller_username, start_time, end_time, status) 
VALUES ('Antique Vase', 'Ming dynasty style vase.', 'COLLECTIBLES', 150.0, 150.0, 'seller1', '2026-05-21T10:00:00Z', '2026-05-30T10:00:00Z', 'ACTIVE');
