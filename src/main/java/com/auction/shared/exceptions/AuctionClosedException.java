package com.auction.shared.exceptions;

/** Thrown when bidding on a non-ACTIVE auction or one past its end time. */
public class AuctionClosedException extends AuctionException {
    public AuctionClosedException(String message) {
        super(message);
    }
}
