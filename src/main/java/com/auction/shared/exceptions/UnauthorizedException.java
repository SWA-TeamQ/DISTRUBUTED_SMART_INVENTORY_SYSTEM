package com.auction.shared.exceptions;

public class UnauthorizedException extends AuctionException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
