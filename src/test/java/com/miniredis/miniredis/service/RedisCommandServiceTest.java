package com.miniredis.miniredis.service;

import com.miniredis.miniredis.domain.RedisDataStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class RedisCommandServiceTest {

    private RedisDataStore dataStore;
    private RedisCommandService commandService;

    @BeforeEach
    void setUp() {
        dataStore = new RedisDataStore();
        commandService = new RedisCommandService(dataStore);
    }

    @AfterEach
    void tearDown() {
        dataStore.shutdown();
    }

}