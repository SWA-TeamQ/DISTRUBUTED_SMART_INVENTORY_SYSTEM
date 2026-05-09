package com.auction.shared.enums;

/**
 * Auction lifecycle states.
 * State machine: ACTIVE -> SOLD | EXPIRED | CANCELLED. EXPIRED -> ACTIVE (relist).
 */
public enum AuctionStatus {
    ACTIVE,
    SOLD,
    EXPIRED,
    CANCELLED
}
