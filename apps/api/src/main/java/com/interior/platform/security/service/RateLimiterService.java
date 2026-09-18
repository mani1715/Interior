package com.interior.platform.security.service;

import com.interior.platform.common.exception.RateLimitExceededException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final Clock clock;
    private final Map<String, Deque<Instant>> requestBuckets = new ConcurrentHashMap<>();

    public RateLimiterService(Clock clock) {
        this.clock = clock;
    }

    /**
     * Checks if a key has exceeded max allowed requests within the given duration window.
     * Throws RateLimitExceededException if exceeded, otherwise records the attempt.
     */
    public synchronized void acquire(String key, int maxRequests, Duration window) {
        Instant now = clock.instant();
        Instant cutoff = now.minus(window);

        Deque<Instant> timestamps = requestBuckets.computeIfAbsent(key, k -> new ArrayDeque<>());

        // Evict expired entries
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= maxRequests) {
            throw new RateLimitExceededException("Too many requests. Please wait before trying again.");
        }

        timestamps.addLast(now);
    }

    /**
     * Helper to clear buckets, useful for test isolation.
     */
    public void reset() {
        requestBuckets.clear();
    }
}
