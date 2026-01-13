package com.miniredis.miniredis.domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RedisDataStoreTest {


    private RedisDataStore dataStore;

    @BeforeEach
    void setUp() {
        dataStore = new RedisDataStore();
    }

    @AfterEach
    void tearDown() {
        dataStore.shutdown();
    }

    @Test
    @DisplayName("SET and GET should store and retrieve values")
    void setAndGet() {
        dataStore.set("key1", "value1");
        Optional<String> result = dataStore.get("key1");

        assertTrue(result.isPresent());
        assertEquals("value1", result.get());
    }

    @Test
    @DisplayName("GET on non-existent key should return empty")
    void getNonExistent() {
        Optional<String> result = dataStore.get("nonexistent");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("SET should overwrite existing values")
    void setOverwrite() {
        dataStore.set("key1", "value1");
        dataStore.set("key1", "value2");

        assertEquals("value2", dataStore.get("key1").orElseThrow());
    }

    @Test
    @DisplayName("SET with EX should expire after specified seconds")
    void setWithExpiry() throws InterruptedException {
        dataStore.setWithExpiry("key1", "value1", 1);

        assertTrue(dataStore.get("key1").isPresent());

        Thread.sleep(1500);

        assertTrue(dataStore.get("key1").isEmpty());
    }

    @Test
    @DisplayName("DEL should remove existing key and return true")
    void deleteExisting() {
        dataStore.set("key1", "value1");
        assertTrue(dataStore.delete("key1"));
        assertTrue(dataStore.get("key1").isEmpty());
    }

    @Test
    @DisplayName("DEL on non-existent key should return false")
    void deleteNonExistent() {
        assertFalse(dataStore.delete("nonexistent"));
    }

    @Test
    @DisplayName("DBSIZE should return correct count")
    void dbSize() {
        assertEquals(0, dataStore.dbSize());

        dataStore.set("key1", "value1");
        assertEquals(1, dataStore.dbSize());

        dataStore.set("key2", "value2");
        assertEquals(2, dataStore.dbSize());

        dataStore.delete("key1");
        assertEquals(1, dataStore.dbSize());
    }

    @Test
    @DisplayName("INCR on non-existent key should set to 1")
    void incrNonExistent() {
        assertEquals(1, dataStore.incr("counter"));
    }

    @Test
    @DisplayName("INCR should increment existing numeric value")
    void incrExisting() {
        dataStore.set("counter", "10");
        assertEquals(11, dataStore.incr("counter"));
    }

    @Test
    @DisplayName("INCR on non-numeric value should throw exception")
    void incrNonNumeric() {
        dataStore.set("key1", "notanumber");
        assertThrows(IllegalArgumentException.class, () -> dataStore.incr("key1"));
    }

    // ============== ZADD Tests ==============

    @Test
    @DisplayName("ZADD should add new member and return 1")
    void zaddNewMember() {
        assertEquals(1, dataStore.zadd("myset", 1.0, "member1"));
    }

    @Test
    @DisplayName("ZADD should update existing member and return 0")
    void zaddUpdateMember() {
        dataStore.zadd("myset", 1.0, "member1");
        assertEquals(0, dataStore.zadd("myset", 2.0, "member1"));
    }

    @Test
    @DisplayName("ZADD on string key should throw exception")
    void zaddOnStringKey() {
        dataStore.set("stringkey", "value");
        assertThrows(IllegalArgumentException.class,
                () -> dataStore.zadd("stringkey", 1.0, "member"));
    }

    // ============== ZCARD Tests ==============

    @Test
    @DisplayName("ZCARD should return cardinality of sorted set")
    void zcard() {
        assertEquals(0, dataStore.zcard("myset"));

        dataStore.zadd("myset", 1.0, "member1");
        assertEquals(1, dataStore.zcard("myset"));

        dataStore.zadd("myset", 2.0, "member2");
        dataStore.zadd("myset", 3.0, "member3");
        assertEquals(3, dataStore.zcard("myset"));
    }

    // ============== ZRANK Tests ==============

    @Test
    @DisplayName("ZRANK should return rank by score order")
    void zrank() {
        dataStore.zadd("myset", 3.0, "three");
        dataStore.zadd("myset", 1.0, "one");
        dataStore.zadd("myset", 2.0, "two");

        assertEquals(0, dataStore.zrank("myset", "one").orElseThrow());
        assertEquals(1, dataStore.zrank("myset", "two").orElseThrow());
        assertEquals(2, dataStore.zrank("myset", "three").orElseThrow());
    }

    @Test
    @DisplayName("ZRANK should return empty for non-existent member")
    void zrankNonExistent() {
        dataStore.zadd("myset", 1.0, "member1");
        assertTrue(dataStore.zrank("myset", "nonexistent").isEmpty());
    }

    // ============== ZRANGE Tests ==============

    @Test
    @DisplayName("ZRANGE should return members in score order")
    void zrange() {
        dataStore.zadd("myset", 3.0, "three");
        dataStore.zadd("myset", 1.0, "one");
        dataStore.zadd("myset", 2.0, "two");

        List<String> result = dataStore.zrange("myset", 0, -1);
        assertEquals(List.of("one", "two", "three"), result);
    }

    @Test
    @DisplayName("ZRANGE with partial range")
    void zrangePartial() {
        dataStore.zadd("myset", 1.0, "a");
        dataStore.zadd("myset", 2.0, "b");
        dataStore.zadd("myset", 3.0, "c");
        dataStore.zadd("myset", 4.0, "d");

        assertEquals(List.of("b", "c"), dataStore.zrange("myset", 1, 2));
    }

    @Test
    @DisplayName("ZRANGE with negative indices")
    void zrangeNegativeIndices() {
        dataStore.zadd("myset", 1.0, "a");
        dataStore.zadd("myset", 2.0, "b");
        dataStore.zadd("myset", 3.0, "c");

        assertEquals(List.of("b", "c"), dataStore.zrange("myset", -2, -1));
    }

    @Test
    @DisplayName("ZRANGE on empty set should return empty list")
    void zrangeEmpty() {
        assertEquals(List.of(), dataStore.zrange("myset", 0, -1));
    }

    @Test
    @DisplayName("ZRANGE with redis Page Test")
    void redisPageTest() {
        dataStore.zadd("myset", 1.0, "one");
        dataStore.zadd("myset", 1.0, "uno");
        dataStore.zadd("myset", 2.0, "two");
        dataStore.zadd("myset", 3.0, "three");

        assertEquals(List.of("one", "uno","two","three"), dataStore.zrange("myset", 0, -1));
    }

    // ============== Concurrent Access Tests ==============

    @Test
    @DisplayName("Concurrent SET operations should be atomic")
    void concurrentSet() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        dataStore.set("key" + threadId + "_" + j, "value" + j);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(threadCount * operationsPerThread, dataStore.dbSize());
    }

    @Test
    @DisplayName("Concurrent INCR operations should be atomic")
    void concurrentIncr() throws InterruptedException {
        int threadCount = 10;
        int incrementsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        dataStore.incr("counter");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(threadCount * incrementsPerThread,
                Long.parseLong(dataStore.get("counter").orElseThrow()));
    }

    @Test
    @DisplayName("Concurrent ZADD operations should be atomic")
    void concurrentZadd() throws InterruptedException {
        int threadCount = 10;
        int addsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger totalAdded = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < addsPerThread; j++) {
                        int result = dataStore.zadd("myset",
                                threadId * 1000 + j,
                                "member_" + threadId + "_" + j);
                        totalAdded.addAndGet(result);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(threadCount * addsPerThread, dataStore.zcard("myset"));
        assertEquals(threadCount * addsPerThread, totalAdded.get());
    }

    @Test
    @DisplayName("Mixed concurrent operations should be safe")
    void concurrentMixedOperations() throws InterruptedException {
        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Writers
        for (int i = 0; i < 4; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 50; j++) {
                        dataStore.set("shared_key", "value_" + threadId + "_" + j);
                        dataStore.incr("counter");
                        dataStore.zadd("zset", threadId * 100 + j, "m" + threadId + "_" + j);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Readers
        for (int i = 0; i < 4; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 50; j++) {
                        dataStore.get("shared_key");
                        dataStore.dbSize();
                        dataStore.zcard("zset");
                        dataStore.zrange("zset", 0, 10);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify counter has expected value
        assertEquals(200, Long.parseLong(dataStore.get("counter").orElseThrow()));
    }

    // ============== FlushAll Tests ==============

    @Test
    @DisplayName("flushAll should clear all data")
    void flushAll() {
        dataStore.set("key1", "value1");
        dataStore.set("key2", "value2");
        dataStore.zadd("zset", 1.0, "member");

        dataStore.flushAll();

        assertEquals(0, dataStore.dbSize());
        assertTrue(dataStore.get("key1").isEmpty());
        assertEquals(0, dataStore.zcard("zset"));
    }

}