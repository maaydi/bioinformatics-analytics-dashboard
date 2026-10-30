package com.bioinformatics.auditservice.service;

import com.bioinformatics.auditservice.support.AuditIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AuditIdempotencyServiceIntegrationTest extends AuditIntegrationTest {

    @Autowired
    private AuditIdempotencyService service;
    @Autowired
    private StringRedisTemplate redis;

    @Test
    void marksOnlyTheRequestedEventWithANamespacedExpiringKey() {
        var eventId = UUID.randomUUID();
        var key = "audit:log:" + eventId;
        try {
            assertThat(service.isProcessed(eventId)).isFalse();

            service.markProcessed(eventId);

            assertThat(service.isProcessed(eventId)).isTrue();
            assertThat(service.isProcessed(UUID.randomUUID())).isFalse();
            assertThat(redis.opsForValue().get(key)).isEqualTo("done");
            assertThat(redis.getExpire(key, TimeUnit.SECONDS)).isBetween(86390L, 86400L);
        } finally {
            redis.delete(key);
        }
    }

    @Test
    void refreshingAMarkerRestoresItsTwentyFourHourExpiry() {
        var eventId = UUID.randomUUID();
        var key = "audit:log:" + eventId;
        try {
            redis.opsForValue().set(key, "old", Duration.ofMinutes(1));

            service.markProcessed(eventId);

            assertThat(redis.opsForValue().get(key)).isEqualTo("done");
            assertThat(redis.getExpire(key, TimeUnit.SECONDS)).isBetween(86390L, 86400L);
        } finally {
            redis.delete(key);
        }
    }

    @Test
    void anExpiredMarkerAllowsTheEventToBeProcessedAgain() {
        var eventId = UUID.randomUUID();
        var key = "audit:log:" + eventId;
        try {
            service.markProcessed(eventId);
            assertThat(redis.expire(key, Duration.ZERO)).isTrue();

            assertThat(service.isProcessed(eventId)).isFalse();
        } finally {
            redis.delete(key);
        }
    }
}
