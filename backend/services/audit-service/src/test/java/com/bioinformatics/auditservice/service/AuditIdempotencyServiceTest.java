package com.bioinformatics.auditservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditIdempotencyServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @InjectMocks
    private AuditIdempotencyService service;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @NullSource
    void onlyAnExistingNamespacedKeyIsConsideredProcessed(Boolean exists) {
        var eventId = UUID.randomUUID();
        when(redisTemplate.hasKey("audit:log:" + eventId)).thenReturn(exists);

        assertThat(service.isProcessed(eventId)).isEqualTo(Boolean.TRUE.equals(exists));
    }

    @Test
    void storesTheCompletionMarkerWithATwentyFourHourExpiry() {
        var eventId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service.markProcessed(eventId);

        verify(valueOperations).set("audit:log:" + eventId, "done", Duration.ofHours(24));
    }

    @Test
    void propagatesRedisLookupFailures() {
        var eventId = UUID.randomUUID();
        var failure = new DataAccessResourceFailureException("Redis unavailable");
        when(redisTemplate.hasKey("audit:log:" + eventId)).thenThrow(failure);

        assertThatThrownBy(() -> service.isProcessed(eventId)).isSameAs(failure);
    }

    @Test
    void propagatesRedisWriteFailures() {
        var eventId = UUID.randomUUID();
        var failure = new DataAccessResourceFailureException("Redis unavailable");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(failure).when(valueOperations).set("audit:log:" + eventId, "done", Duration.ofHours(24));

        assertThatThrownBy(() -> service.markProcessed(eventId)).isSameAs(failure);
    }
}
