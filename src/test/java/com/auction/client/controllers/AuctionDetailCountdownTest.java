package com.auction.client.controllers;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuctionDetailCountdownTest {

    @Test
    public void testFormatTimeLeftUsesProvidedAdjustedServerTime() {
        Instant adjustedNow = Instant.parse("2026-05-25T12:00:00Z");
        String endTime = adjustedNow.plusSeconds(3720).toString();

        String result = AuctionDetailController.formatTimeLeft(adjustedNow, endTime);

        assertEquals("01h 02m", result);
    }
}
