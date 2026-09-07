package com.frauddetect.starter.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Tracks how many transactions a user makes within a short period.
 *
 * Redis is used because it is fast and suitable for temporary counters.
 */
@Service
public class VelocityCheckService {

    private static final int MAX_TRANSACTIONS_PER_MINUTE = 3;
    private static final long WINDOW_SECONDS = 60;

    private final StringRedisTemplate redisTemplate;

    public VelocityCheckService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Increments the user's transaction counter.
     *
     * Returns true when the user has made more than
     * 3 transactions within the current 1-minute window.
     */
    public boolean isVelocityExceeded(String userId) {

        String key = "fraud:velocity:" + userId;

        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(
                    key,
                    WINDOW_SECONDS,
                    TimeUnit.SECONDS
            );
        }

        return count != null && count > MAX_TRANSACTIONS_PER_MINUTE;
    }
}