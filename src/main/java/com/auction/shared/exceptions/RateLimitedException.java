package com.auction.shared.exceptions;

public class RateLimitedException extends AuctionException {
    public RateLimitedException(String message) {
        super(message);
    }
}
