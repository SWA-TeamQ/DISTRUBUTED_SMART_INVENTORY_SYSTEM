package com.auction.server.core.logging;

import java.time.Instant;

public class LogEntry {
    private final Instant timestamp;
    private final LogCategory category;
    private final EventType eventType;
    private final String details;

    public LogEntry(LogCategory category, EventType eventType, String details) {
        this.timestamp = Instant.now();
        this.category = category;
        this.eventType = eventType;
        this.details = details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public LogCategory getCategory() {
        return category;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getDetails() {
        return details;
    }
}
