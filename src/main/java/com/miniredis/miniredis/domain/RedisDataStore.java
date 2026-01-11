package com.miniredis.miniredis.domain;

import org.springframework.stereotype.Component;


import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class RedisDataStore {

    private final ConcurrentHashMap<String, RedisValue> store = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReadWriteLock> keyLocks = new ConcurrentHashMap<>();

    private final ScheduledExecutorService cleanupExecutor;

    public RedisDataStore() {
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "redis-cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredKeys, 1, 1, TimeUnit.SECONDS);
    }

    private ReadWriteLock getLock(String key) {
        return keyLocks.computeIfAbsent(key, k -> new ReentrantReadWriteLock());
    }

    /**
     * SET key value
     */
    public void set(String key, String value) {
        var lock = getLock(key).writeLock();
        lock.lock();
        try {
            store.put(key, new RedisValue.StringValue(value));
        } finally {
            lock.unlock();
        }
    }

    /**
     * SET key value EX seconds
     */
    public void setWithExpiry(String key, String value, long seconds) {
        var lock = getLock(key).writeLock();
        lock.lock();
        try {
            store.put(key, new RedisValue.StringValue(value, seconds));
        } finally {
            lock.unlock();
        }
    }

    /**
     * GET key
     * @return Optional containing the value, or empty if key doesn't exist or is expired
     */
    public Optional<String> get(String key) {
        var lock = getLock(key).readLock();
        lock.lock();
        try {
            RedisValue value = store.get(key);
            if (value == null) {
                return Optional.empty();
            }

            if (value.isExpired()) {
                lock.unlock();
                var writeLock = getLock(key).writeLock();
                writeLock.lock();
                try {
                    // Double-check the value is still expired
                    value = store.get(key);
                    if (value != null && value.isExpired()) {
                        store.remove(key);
                        keyLocks.remove(key);
                    }
                    return Optional.empty();
                } finally {
                    writeLock.unlock();
                }
            }

            if (value instanceof RedisValue.StringValue sv) {
                return Optional.of(sv.value());
            }

            return Optional.empty(); // Wrong type
        } finally {
            if (((ReentrantReadWriteLock) getLock(key)).getReadHoldCount() > 0) {
                lock.unlock();
            }
        }
    }

    /**
     * Clean up expired keys
     */
    private void cleanupExpiredKeys() {
        store.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired()) {
                keyLocks.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * Clear all data (for testing purposes)
     */
    public void flushAll() {
        store.clear();
        keyLocks.clear();
    }

    /**
     * Shutdown the cleanup executor
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}
