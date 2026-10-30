package com.bioinformatics.auditservice.service;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditIdempotencyService {

    private static final String KEY_PREFIX = "audit:log:";
    private static final Duration TTL = Duration.ofHours(24);
    private final StringRedisTemplate redisTemplate;

    public boolean isProcessed(UUID eventId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + eventId.toString()));
    }

    public void markProcessed(UUID eventId) {
        redisTemplate.opsForValue().set(KEY_PREFIX + eventId.toString(), "done", TTL);
    }
}