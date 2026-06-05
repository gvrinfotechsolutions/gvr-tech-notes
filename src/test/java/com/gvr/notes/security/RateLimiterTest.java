package com.gvr.notes.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter();
    }

    @Test
    void allowsRequestsUnderLimit() {
        for (int i = 0; i < 10; i++) {
            assertThat(rateLimiter.isAllowed("1.2.3.4")).isTrue();
        }
    }

    @Test
    void blocksAfterMaxAttempts() {
        for (int i = 0; i < 10; i++) {
            rateLimiter.isAllowed("5.5.5.5");
        }
        assertThat(rateLimiter.isAllowed("5.5.5.5")).isFalse();
    }

    @Test
    void differentIpsAreTrackedIndependently() {
        for (int i = 0; i < 10; i++) {
            rateLimiter.isAllowed("10.0.0.1");
        }
        // 10.0.0.2 should still be allowed — its counter is separate
        assertThat(rateLimiter.isAllowed("10.0.0.2")).isTrue();
    }

    @Test
    void resetClearsCounterForIp() {
        for (int i = 0; i < 11; i++) {
            rateLimiter.isAllowed("9.9.9.9");
        }
        assertThat(rateLimiter.isAllowed("9.9.9.9")).isFalse();

        rateLimiter.reset("9.9.9.9");

        assertThat(rateLimiter.isAllowed("9.9.9.9")).isTrue();
    }
}
