package com.gvr.notes.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory rate limiter for auth endpoints.
 * Tracks attempts per IP and blocks after MAX_ATTEMPTS within the window.
 * A scheduled cleanup prevents unbounded memory growth.
 */
@Component
@Slf4j
public class RateLimiter {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_SECONDS = 60;

    private record Bucket(int count, Instant windowStart) {}

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Returns true if the IP is allowed to proceed; false if it is rate-limited.
     */
    public boolean isAllowed(String ip) {
        Instant now = Instant.now();
        Bucket bucket = buckets.compute(ip, (key, existing) -> {
            if (existing == null || now.getEpochSecond() - existing.windowStart().getEpochSecond() > WINDOW_SECONDS) {
                return new Bucket(1, now);
            }
            return new Bucket(existing.count() + 1, existing.windowStart());
        });

        if (bucket.count() > MAX_ATTEMPTS) {
            log.warn("Rate limit exceeded for ip={}", ip);
            return false;
        }
        return true;
    }

    /** Expose for testing */
    public void reset(String ip) {
        buckets.remove(ip);
    }
}
