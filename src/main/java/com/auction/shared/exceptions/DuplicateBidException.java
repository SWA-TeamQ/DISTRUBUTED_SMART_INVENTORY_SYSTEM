package com.auction.shared.exceptions;

/** Thrown when the current highest bidder tries to bid again. */
public class DuplicateBidException extends AuctionException {
    public DuplicateBidException(String message) {
        super(message);
    }
}
