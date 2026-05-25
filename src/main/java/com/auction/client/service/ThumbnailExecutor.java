package com.auction.client.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ThumbnailExecutor {
    private static final ExecutorService EXEC = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("thumbnail-loader-" + t.getId());
        return t;
    });

    private ThumbnailExecutor() {}

    public static ExecutorService getExecutor() {
        return EXEC;
    }
}
