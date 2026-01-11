package com.miniredis.miniredis.domain;

import java.time.Instant;

public sealed interface RedisValue permits RedisValue.StringValue {

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

}
