package com.interior.platform.security.service;

import com.interior.platform.common.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {

    private RateLimiterService rateLimiter;
    private AtomicReference<Instant> currentTime;

    @BeforeEach
    void setUp() {
        currentTime = new AtomicReference<>(Instant.parse("2026-09-18T12:00:00Z"));
        java.time.Clock mutableClock = new java.time.Clock() {
            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public java.time.Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return currentTime.get();
            }
        };

        rateLimiter = new RateLimiterService(mutableClock);
    }

    @Test
    @DisplayName("Requests within allowed threshold are accepted without exception")
    void testRequestsWithinLimitAllowed() {
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 5; i++) {
                rateLimiter.acquire("client-1", 5, Duration.ofMinutes(1));
            }
        });
    }

    @Test
    @DisplayName("Requests exceeding threshold within window throw RateLimitExceededException (429)")
    void testRequestsExceedingLimitRejected() {
        for (int i = 0; i < 5; i++) {
            rateLimiter.acquire("client-1", 5, Duration.ofMinutes(1));
        }

        RateLimitExceededException ex = assertThrows(
                RateLimitExceededException.class,
                () -> rateLimiter.acquire("client-1", 5, Duration.ofMinutes(1))
        );

        assertTrue(ex.getMessage().contains("Too many requests"));
    }

    @Test
    @DisplayName("Rate limiter isolates different client keys")
    void testClientKeyIsolation() {
        for (int i = 0; i < 5; i++) {
            rateLimiter.acquire("attacker-ip", 5, Duration.ofMinutes(1));
        }

        // Attacker is blocked
        assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.acquire("attacker-ip", 5, Duration.ofMinutes(1)));

        // Legitimate client is NOT blocked
        assertDoesNotThrow(() -> rateLimiter.acquire("legit-ip", 5, Duration.ofMinutes(1)));
    }

    @Test
    @DisplayName("Rate limiter resets/recovers after duration window has elapsed")
    void testWindowRecovery() {
        for (int i = 0; i < 3; i++) {
            rateLimiter.acquire("client-1", 3, Duration.ofSeconds(60));
        }

        // 4th request blocked
        assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.acquire("client-1", 3, Duration.ofSeconds(60)));

        // Advance time by 61 seconds
        currentTime.set(currentTime.get().plusSeconds(61));

        // Now request is allowed again
        assertDoesNotThrow(() -> rateLimiter.acquire("client-1", 3, Duration.ofSeconds(60)));
    }
}
