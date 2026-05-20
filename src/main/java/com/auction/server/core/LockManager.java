package com.auction.server.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Reusable concurrency module that manages locks per auction.
 */
public class LockManager {
    private final ConcurrentHashMap<Integer, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    public void lock(int auctionId) {
        ReentrantLock lock = auctionLocks.computeIfAbsent(auctionId, id -> new ReentrantLock());
        lock.lock();
    }

    public void unlock(int auctionId) {
        ReentrantLock lock = auctionLocks.get(auctionId);
        if (lock != null) {
            lock.unlock();
        }
    }
}
