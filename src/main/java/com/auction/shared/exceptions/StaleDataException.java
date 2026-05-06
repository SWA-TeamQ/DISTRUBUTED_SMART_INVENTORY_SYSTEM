package com.auction.shared.exceptions;

/**
 * Thrown when client's expected price doesn't match server's current bid.
 * Forces client to refresh before retrying.
 */
public class StaleDataException extends AuctionException {
    public StaleDataException(String message) {
        super(message);
    }
}
