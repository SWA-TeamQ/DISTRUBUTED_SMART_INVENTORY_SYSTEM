package com.auction.shared.exceptions;

/** Thrown when a seller tries to bid on their own auction. */
public class SelfBidException extends AuctionException {
    public SelfBidException(String message) {
        super(message);
    }
}
