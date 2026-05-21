-- Seed data for RTDAS
INSERT INTO users (username, password, role) VALUES ('admin', 'admin', 'ADMIN');
INSERT INTO users (username, password, role) VALUES ('seller1', 'password123', 'SELLER');
INSERT INTO users (username, password, role) VALUES ('bidder1', 'password123', 'BIDDER');

INSERT INTO auction_items (title, description, starting_price, seller_id, status) 
VALUES ('Vintage Clock', 'A beautiful 19th-century clock.', 5000, 2, 'ACTIVE');

INSERT INTO auction_items (title, description, starting_price, seller_id, status) 
VALUES ('Antique Vase', 'Ming dynasty style vase.', 15000, 2, 'ACTIVE');
