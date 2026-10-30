package com.bioinformatics.auditservice.service;

import com.bioinformatics.auditservice.repository.AuditLogRepository;
import com.bioinformatics.auditservice.support.AuditIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.transaction.annotation.Transactional;

import static com.bioinformatics.auditservice.support.AuditTestFixtures.event;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@Transactional
class AuditConsumerIntegrationTest extends AuditIntegrationTest {

    @Autowired
    private AuditConsumer consumer;
    @Autowired
    private AuditLogRepository repository;
    @Autowired
    private AuditIdempotencyService idempotencyService;
    @Autowired
    private StringRedisTemplate redis;

    @Test
    void redeliveredEventsAreAcknowledgedButOnlyPersistedOnce() {
        var event = event("endpoint", "/login");
        var acknowledgment = mock(Acknowledgment.class);
        var key = "audit:log:" + event.eventId();
        try {
            consumer.consume(event, acknowledgment);
            repository.flush();
            consumer.consume(event, acknowledgment);

            assertThat(repository.findAll()).singleElement().satisfies(saved -> {
                assertThat(saved.getEventId()).isEqualTo(event.eventId());
                assertThat(saved.getMetadata()).containsEntry("endpoint", "/login");
            });
            assertThat(idempotencyService.isProcessed(event.eventId())).isTrue();
            verify(acknowledgment, times(2)).acknowledge();
        } finally {
            redis.delete(key);
        }
    }

    @Test
    void rejectedEventsAreNotPersistedMarkedOrAcknowledged() {
        var event = event("unpaired");
        var acknowledgment = mock(Acknowledgment.class);

        assertThatThrownBy(() -> consumer.consume(event, acknowledgment))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(repository.count()).isZero();
        assertThat(idempotencyService.isProcessed(event.eventId())).isFalse();
        verifyNoInteractions(acknowledgment);
    }
}
