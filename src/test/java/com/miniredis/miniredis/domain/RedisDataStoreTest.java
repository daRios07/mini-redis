package com.miniredis.miniredis.domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

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
}