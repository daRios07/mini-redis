package com.miniredis.miniredis.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;

public sealed interface RedisValue permits RedisValue.StringValue, RedisValue.SortedSetValue {

    boolean isExpired();

    record StringValue(String value, Instant expiresAt) implements RedisValue {

        public StringValue(String value) {
            this(value, null);
        }

        public StringValue(String value, long expirationSeconds) {
            this(value, Instant.now().plusSeconds(expirationSeconds));
        }

        @Override
        public boolean isExpired() {
            return expiresAt != null && Instant.now().isAfter(expiresAt);
        }

        public java.util.Optional<Instant> getExpiresAt() {
            return java.util.Optional.ofNullable(expiresAt);
        }
    }

    /**
     * Sorted set value - stores members with scores
     * Uses ConcurrentSkipListMap for thread-safe ordered access
     */
    record SortedSetValue(ConcurrentSkipListMap<String, Double> members) implements RedisValue {

        public SortedSetValue() {
            this(new ConcurrentSkipListMap<>());
        }

        @Override
        public boolean isExpired() {
            return false;
        }


        public boolean addMember(String member, double score) {
            return members.put(member, score) == null;
        }

        public Optional<Double> getScore(String member) {
            return Optional.ofNullable(members.get(member));
        }

        public int cardinality() {
            return members.size();
        }

        public Optional<Integer> getRank(String member) {
            if (!members.containsKey(member)) {
                return Optional.empty();
            }

            Double targetScore = members.get(member);
            int rank = 0;

            for (var entry : members.entrySet()) {
                int scoreCompare = Double.compare(entry.getValue(), targetScore);
                if (scoreCompare < 0) {
                    rank++;
                } else if (scoreCompare == 0 && entry.getKey().compareTo(member) < 0) {
                    rank++;
                }
            }

            return Optional.of(rank);
        }

        public java.util.List<String> getRange(int start, int stop) {
            int size = members.size();
            if (size == 0) {
                return java.util.List.of();
            }

            // Normalize negative indices
            int normalizedStart = start < 0 ? size + start : start;
            int normalizedStop = stop < 0 ? size + stop : stop;

            // Clamp to valid range
            normalizedStart = Math.max(0, normalizedStart);
            normalizedStop = Math.min(size - 1, normalizedStop);

            if (normalizedStart > normalizedStop || normalizedStart >= size) {
                return java.util.List.of();
            }

            // Sort by score, then by member name
            return members.entrySet().stream()
                    .sorted(Comparator.comparingDouble((Map.Entry<String, Double> e) -> e.getValue()).thenComparing(Map.Entry::getKey))
                    .skip(normalizedStart)
                    .limit(normalizedStop - normalizedStart + 1)
                    .map(java.util.Map.Entry::getKey)
                    .toList();
        }
    }
}
