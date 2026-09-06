package com.bioinformatics.analyticsservice.materializeviews.service;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshIdempotencyService {

    private static final String KEY_PREFIX = "refresh:job:";
    private static final Duration TTL = Duration.ofHours(24);
    private final StringRedisTemplate redisTemplate;

    public boolean isProcessed(String jobId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jobId));
    }

    public void markProcessed(String jobId) {
        redisTemplate.opsForValue().set(KEY_PREFIX + jobId, "done", TTL);
    }
}