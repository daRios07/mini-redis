package com.miniredis.miniredis.domain;

import org.springframework.stereotype.Component;


import java.util.List;
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

    public boolean delete(String key) {
        var lock = getLock(key).writeLock();
        lock.lock();
        try {
            RedisValue removed = store.remove(key);
            if (removed != null) {
                keyLocks.remove(key);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public int dbSize() {
        // Clean expired keys first for accurate count
        cleanupExpiredKeys();
        return store.size();
    }


    public long incr(String key) {
        var lock = getLock(key).writeLock();
        lock.lock();
        try {
            RedisValue value = store.get(key);
            long currentValue = 0;

            if (value != null) {
                if (value.isExpired()) {
                    store.remove(key);
                } else if (value instanceof RedisValue.StringValue sv) {
                    try {
                        currentValue = Long.parseLong(sv.value());
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("ERR value is not an integer or out of range");
                    }
                } else {
                    throw new IllegalArgumentException("WRONGTYPE Operation against a key holding the wrong kind of value");
                }
            }

            long newValue = currentValue + 1;
            store.put(key, new RedisValue.StringValue(String.valueOf(newValue)));
            return newValue;
        } finally {
            lock.unlock();
        }
    }


    /**
     * ZADD key score member
     * Adds a member with the specified score to the sorted set.
     * @return 1 if the member was added, 0 if it was updated
     */
    public int zadd(String key, double score, String member) {
        var lock = getLock(key).writeLock();
        lock.lock();
        try {
            RedisValue value = store.get(key);
            RedisValue.SortedSetValue sortedSet;

            if (value == null) {
                sortedSet = new RedisValue.SortedSetValue();
                store.put(key, sortedSet);
            } else if (value instanceof RedisValue.SortedSetValue ss) {
                sortedSet = ss;
            } else {
                throw new IllegalArgumentException("WRONGTYPE Operation against a key holding the wrong kind of value");
            }

            return sortedSet.addMember(member, score) ? 1 : 0;
        } finally {
            lock.unlock();
        }
    }

    /**
     * ZCARD key
     * @return the number of members in the sorted set, or 0 if key doesn't exist
     */
    public int zcard(String key) {
        var lock = getLock(key).readLock();
        lock.lock();
        try {
            RedisValue value = store.get(key);
            if (value == null) {
                return 0;
            }
            if (value instanceof RedisValue.SortedSetValue ss) {
                return ss.cardinality();
            }
            throw new IllegalArgumentException("WRONGTYPE Operation against a key holding the wrong kind of value");
        } finally {
            lock.unlock();
        }
    }

    /**
     * ZRANK key member
     * @return Optional containing the rank of the member, or empty if member doesn't exist
     */
    public Optional<Integer> zrank(String key, String member) {
        var lock = getLock(key).readLock();
        lock.lock();
        try {
            RedisValue value = store.get(key);
            if (value == null) {
                return Optional.empty();
            }
            if (value instanceof RedisValue.SortedSetValue ss) {
                return ss.getRank(member);
            }
            throw new IllegalArgumentException("WRONGTYPE Operation against a key holding the wrong kind of value");
        } finally {
            lock.unlock();
        }
    }

    /**
     * ZRANGE key start stop
     * @return list of members in the specified range
     */
    public List<String> zrange(String key, int start, int stop) {
        var lock = getLock(key).readLock();
        lock.lock();
        try {
            RedisValue value = store.get(key);
            if (value == null) {
                return List.of();
            }
            if (value instanceof RedisValue.SortedSetValue ss) {
                return ss.getRange(start, stop);
            }
            throw new IllegalArgumentException("WRONGTYPE Operation against a key holding the wrong kind of value");
        } finally {
            lock.unlock();
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
