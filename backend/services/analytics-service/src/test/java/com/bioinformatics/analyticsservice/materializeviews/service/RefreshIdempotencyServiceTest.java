package com.bioinformatics.analyticsservice.materializeviews.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RefreshIdempotencyService}.
 * Tests idempotency tracking using Redis for preventing duplicate view refresh processing.
 */
@ExtendWith(MockitoExtension.class)
class RefreshIdempotencyServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private RefreshIdempotencyService service;

    @BeforeEach
    void setUp() {
        service = new RefreshIdempotencyService(redisTemplate);
    }

    @Test
    void isProcessed_withExistingKey_returnsTrue() {
        // Arrange
        var jobId = "job-123";
        when(redisTemplate.hasKey("refresh:job:" + jobId))
                .thenReturn(true);

        // Act
        var result = service.isProcessed(jobId);

        // Assert
        assertThat(result).isTrue();
        verify(redisTemplate).hasKey("refresh:job:" + jobId);
    }

    @Test
    void isProcessed_withNonExistentKey_returnsFalse() {
        // Arrange
        var jobId = "job-456";
        when(redisTemplate.hasKey("refresh:job:" + jobId))
                .thenReturn(false);

        // Act
        var result = service.isProcessed(jobId);

        // Assert
        assertThat(result).isFalse();
        verify(redisTemplate).hasKey("refresh:job:" + jobId);
    }

    @Test
    void isProcessed_withNullKey_returnsFalse() {
        // Arrange
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        // Act
        var result = service.isProcessed("nonexistent");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void markProcessed_setsKeyWithCorrectTTL() {
        // Arrange
        var jobId = "job-789";
        var expectedKey = "refresh:job:" + jobId;
        var expectedTtl = Duration.ofHours(24);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        // Act
        service.markProcessed(jobId);

        // Assert
        verify(valueOps).set(eq(expectedKey), eq("done"), eq(expectedTtl));
    }

    @Test
    void markProcessed_withUUID_handlesCorrectly() {
        // Arrange
        var jobId = UUID.randomUUID().toString();
        var expectedKey = "refresh:job:" + jobId;
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        // Act
        service.markProcessed(jobId);

        // Assert
        verify(valueOps).set(eq(expectedKey), eq("done"), eq(Duration.ofHours(24)));
    }

    @Test
    void markProcessed_multipleJobIds_createsIndependentKeys() {
        // Arrange
        var jobId1 = "job-1";
        var jobId2 = "job-2";

        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        // Act
        service.markProcessed(jobId1);
        service.markProcessed(jobId2);

        // Assert
        verify(valueOps).set(eq("refresh:job:" + jobId1), eq("done"), eq(Duration.ofHours(24)));
        verify(valueOps).set(eq("refresh:job:" + jobId2), eq("done"), eq(Duration.ofHours(24)));
    }

    @Test
    void isProcessed_afterMarkProcessed_returnsTrue() {
        // Arrange
        var jobId = "job-final";
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.hasKey("refresh:job:" + jobId))
                .thenReturn(false)  // First call to isProcessed
                .thenReturn(true);   // After mark, second call to isProcessed
        // Act
        var before = service.isProcessed(jobId);
        service.markProcessed(jobId);
        var after = service.isProcessed(jobId);

        // Assert
        assertThat(before).isFalse();
        assertThat(after).isTrue();
        verify(redisTemplate, times(2)).hasKey("refresh:job:" + jobId);
        verify(valueOps).set(eq("refresh:job:" + jobId), eq("done"), eq(Duration.ofHours(24)));
    }

    @Test
    void isProcessed_keyPrefix_isConsistent() {
        // Arrange
        var jobId1 = "uniprot-import";
        var jobId2 = "uniprot-import";  // Same ID should use same prefix
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        // Act
        service.isProcessed(jobId1);
        service.isProcessed(jobId2);

        // Assert
        // Both should use the same key prefix
        verify(redisTemplate, times(2)).hasKey("refresh:job:uniprot-import");
    }

    @Test
    void idempotencyService_handlesSpecialCharacterJobIds() {
        // Arrange
        var jobId = "job-123-456-789";
        var expectedKey = "refresh:job:" + jobId;
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        // Act
        service.markProcessed(jobId);

        // Assert
        verify(valueOps).set(eq(expectedKey), eq("done"), eq(Duration.ofHours(24)));
    }
}

