package com.auction.shared.exceptions;

/** Thrown when bid amount is below currentBid * 1.05 (5% min increment). */
public class InsufficientBidException extends AuctionException {
    public InsufficientBidException(String message) {
        super(message);
    }
}
