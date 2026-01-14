package com.miniredis.miniredis.controller;

import com.miniredis.miniredis.MiniRedisApplication;
import com.miniredis.miniredis.service.RedisCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        classes = MiniRedisApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.shell.interactive.enabled=false"
        }
)
class RedisControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RedisCommandService commandService;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        commandService.flushAll();
    }

    // ============== Query Parameter Style Tests ==============

    @Test
    @DisplayName("SET and GET via query parameter should work")
    void setAndGetViaQueryParam() {
        // SET
        ResponseEntity<String> setResponse = restTemplate.getForEntity(
                baseUrl + "/?cmd=SET%20mykey%20cool-value", String.class);
        assertEquals("OK", setResponse.getBody());

        // GET
        ResponseEntity<String> getResponse = restTemplate.getForEntity(
                baseUrl + "/?cmd=GET%20mykey", String.class);
        assertEquals("cool-value", getResponse.getBody());
    }

    @Test
    @DisplayName("DEL via query parameter should work")
    void delViaQueryParam() {
        // SET first
        restTemplate.getForEntity(baseUrl + "/?cmd=SET%20mykey%20value", String.class);

        // DEL
        ResponseEntity<String> delResponse = restTemplate.getForEntity(
                baseUrl + "/?cmd=DEL%20mykey", String.class);
        assertEquals("1", delResponse.getBody());

        // GET should return nil
        ResponseEntity<String> getResponse = restTemplate.getForEntity(
                baseUrl + "/?cmd=GET%20mykey", String.class);
        assertEquals("(nil)", getResponse.getBody());
    }

    // ============== REST Style Tests ==============

    @Test
    @DisplayName("PUT and GET REST style should work")
    void putAndGetRestStyle() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        HttpEntity<String> request = new HttpEntity<>("my-value", headers);

        // PUT
        ResponseEntity<String> putResponse = restTemplate.exchange(
                baseUrl + "/mykey", HttpMethod.PUT, request, String.class);
        assertEquals("OK", putResponse.getBody());

        // GET
        ResponseEntity<String> getResponse = restTemplate.getForEntity(
                baseUrl + "/mykey", String.class);
        assertEquals("my-value", getResponse.getBody());
    }

    @Test
    @DisplayName("DELETE REST style should work")
    void deleteRestStyle() {
        // SET first
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        restTemplate.exchange(baseUrl + "/mykey", HttpMethod.PUT,
                new HttpEntity<>("value", headers), String.class);

        // DELETE
        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                baseUrl + "/mykey", HttpMethod.DELETE, null, String.class);
        assertEquals("1", deleteResponse.getBody());

        // GET should return nil
        ResponseEntity<String> getResponse = restTemplate.getForEntity(
                baseUrl + "/mykey", String.class);
        assertEquals("(nil)", getResponse.getBody());
    }

    // ============== INCR Tests ==============

    @Test
    @DisplayName("INCR should work via REST endpoint")
    void incrRestStyle() {
        // INCR new key
        ResponseEntity<String> response1 = restTemplate.postForEntity(
                baseUrl + "/counter/incr", null, String.class);
        assertEquals("1", response1.getBody());

        // INCR again
        ResponseEntity<String> response2 = restTemplate.postForEntity(
                baseUrl + "/counter/incr", null, String.class);
        assertEquals("2", response2.getBody());
    }

    // ============== Sorted Set Tests ==============

    @Test
    @DisplayName("ZADD, ZCARD, ZRANK, ZRANGE should work")
    void sortedSetOperations() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        // ZADD
        restTemplate.postForEntity(baseUrl + "/myset/zadd",
                new HttpEntity<>("3 three", headers), String.class);
        restTemplate.postForEntity(baseUrl + "/myset/zadd",
                new HttpEntity<>("1 one", headers), String.class);
        restTemplate.postForEntity(baseUrl + "/myset/zadd",
                new HttpEntity<>("2 two", headers), String.class);

        // ZCARD
        ResponseEntity<String> zcardResponse = restTemplate.getForEntity(
                baseUrl + "/myset/zcard", String.class);
        assertEquals("3", zcardResponse.getBody());

        // ZRANK
        ResponseEntity<String> zrankResponse = restTemplate.getForEntity(
                baseUrl + "/myset/zrank/two", String.class);
        assertEquals("1", zrankResponse.getBody());

        // ZRANGE
        ResponseEntity<String> zrangeResponse = restTemplate.getForEntity(
                baseUrl + "/myset/zrange?start=0&stop=-1", String.class);
        assertEquals("one two three", zrangeResponse.getBody());
    }

    // ============== DBSIZE Tests ==============

    @Test
    @DisplayName("DBSIZE should return correct count")
    void dbSize() {
        // Add some keys
        restTemplate.getForEntity(baseUrl + "/?cmd=SET%20key1%20val1", String.class);
        restTemplate.getForEntity(baseUrl + "/?cmd=SET%20key2%20val2", String.class);

        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/dbsize", String.class);
        assertEquals("2", response.getBody());
    }

    // ============== Concurrent Access Tests ==============

    @Test
    @DisplayName("Concurrent SET requests should all succeed")
    void concurrentSetRequests() throws InterruptedException, ExecutionException {
        int threadCount = 10;
        int requestsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            futures.add(executor.submit(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    String key = "key_" + threadId + "_" + j;
                    String value = "value_" + threadId + "_" + j;
                    ResponseEntity<String> response = restTemplate.getForEntity(
                            baseUrl + "/?cmd=SET%20" + key + "%20" + value, String.class);
                    if (!"OK".equals(response.getBody())) {
                        return false;
                    }
                }
                return true;
            }));
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

        for (Future<Boolean> future : futures) {
            assertTrue(future.get());
        }

        // Verify all keys were set
        ResponseEntity<String> dbsizeResponse = restTemplate.getForEntity(
                baseUrl + "/dbsize", String.class);
        assertEquals(String.valueOf(threadCount * requestsPerThread), dbsizeResponse.getBody());
    }

    @Test
    @DisplayName("Concurrent INCR requests should be atomic")
    void concurrentIncrRequests() throws InterruptedException, ExecutionException {
        int threadCount = 10;
        int incrementsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    restTemplate.postForEntity(baseUrl + "/counter/incr", null, String.class);
                }
                return null;
            }));
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

        // Wait for all futures to complete
        for (Future<Void> future : futures) {
            future.get();
        }

        // Verify final counter value
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/counter", String.class);
        assertEquals(String.valueOf(threadCount * incrementsPerThread), response.getBody());
    }

    @Test
    @DisplayName("Concurrent ZADD requests should be atomic")
    void concurrentZaddRequests() throws InterruptedException, ExecutionException {
        int threadCount = 8;
        int addsPerThread = 25;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Future<Void>> futures = new ArrayList<>();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            futures.add(executor.submit(() -> {
                for (int j = 0; j < addsPerThread; j++) {
                    String member = "member_" + threadId + "_" + j;
                    double score = threadId * 1000 + j;
                    HttpEntity<String> request = new HttpEntity<>(score + " " + member, headers);
                    ResponseEntity<String> response = restTemplate.postForEntity(
                            baseUrl + "/myset/zadd", request, String.class);
                    if ("1".equals(response.getBody())) {
                        successCount.incrementAndGet();
                    }
                }
                return null;
            }));
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

        // Wait for all futures
        for (Future<Void> future : futures) {
            future.get();
        }

        // Verify cardinality
        ResponseEntity<String> zcardResponse = restTemplate.getForEntity(
                baseUrl + "/myset/zcard", String.class);
        assertEquals(String.valueOf(threadCount * addsPerThread), zcardResponse.getBody());
        assertEquals(threadCount * addsPerThread, successCount.get());
    }

    @Test
    @DisplayName("Mixed concurrent read/write operations should be safe")
    void concurrentMixedOperations() throws InterruptedException {
        int threadCount = 12;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        // Writers (4 threads)
        for (int i = 0; i < 4; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 30; j++) {
                        // SET operations
                        restTemplate.exchange(baseUrl + "/shared_" + (j % 5),
                                HttpMethod.PUT,
                                new HttpEntity<>("val_" + threadId + "_" + j, headers),
                                String.class);
                        // INCR operations
                        restTemplate.postForEntity(baseUrl + "/counter/incr", null, String.class);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Readers (4 threads)
        for (int i = 0; i < 4; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 30; j++) {
                        restTemplate.getForEntity(baseUrl + "/shared_" + (j % 5), String.class);
                        restTemplate.getForEntity(baseUrl + "/db/size", String.class);
                        restTemplate.getForEntity(baseUrl + "/counter", String.class);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Sorted set writers (2 threads)
        for (int i = 0; i < 2; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 30; j++) {
                        HttpEntity<String> request = new HttpEntity<>(
                                (threadId * 100 + j) + " m_" + threadId + "_" + j, headers);
                        restTemplate.postForEntity(baseUrl + "/zset/zadd", request, String.class);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Sorted set readers (2 threads)
        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 30; j++) {
                        restTemplate.getForEntity(baseUrl + "/zset/zcard", String.class);
                        restTemplate.getForEntity(baseUrl + "/zset/zrange?start=0&stop=10", String.class);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads
        startLatch.countDown();

        // Wait for completion
        assertTrue(doneLatch.await(60, TimeUnit.SECONDS));
        executor.shutdown();

        // Verify data integrity
        // Counter should have been incremented 4 threads * 30 times = 120
        ResponseEntity<String> counterResponse = restTemplate.getForEntity(
                baseUrl + "/counter", String.class);
        assertEquals("120", counterResponse.getBody());

        // Sorted set should have 2 threads * 30 members = 60
        ResponseEntity<String> zcardResponse = restTemplate.getForEntity(
                baseUrl + "/zset/zcard", String.class);
        assertEquals("60", zcardResponse.getBody());
    }

    @Test
    @DisplayName("Expiry should work correctly")
    void expiryTest() throws InterruptedException {
        // Set with 1 second expiry via query param
        restTemplate.getForEntity(baseUrl + "/?cmd=SET%20expiring%20value%20EX%201", String.class);

        // Should exist immediately
        ResponseEntity<String> response1 = restTemplate.getForEntity(
                baseUrl + "/expiring", String.class);
        assertEquals("value", response1.getBody());

        // Wait for expiry
        Thread.sleep(1500);

        // Should be gone
        ResponseEntity<String> response2 = restTemplate.getForEntity(
                baseUrl + "/expiring", String.class);
        assertEquals("(nil)", response2.getBody());
    }
}