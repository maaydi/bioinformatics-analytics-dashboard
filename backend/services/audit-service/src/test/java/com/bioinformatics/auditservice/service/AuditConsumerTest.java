package com.bioinformatics.auditservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.kafka.support.Acknowledgment;

import static com.bioinformatics.auditservice.support.AuditTestFixtures.event;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditConsumerTest {

    @Mock
    private AuditIdempotencyService idempotencyService;
    @Mock
    private AuditService service;
    @Mock
    private Acknowledgment acknowledgment;
    @InjectMocks
    private AuditConsumer consumer;

    @Test
    void acknowledgesOnlyAfterSavingAndMarkingTheEventProcessed() {
        var event = event();

        consumer.consume(event, acknowledgment);

        var order = inOrder(idempotencyService, service, acknowledgment);
        order.verify(idempotencyService).isProcessed(event.eventId());
        order.verify(service).save(event);
        order.verify(idempotencyService).markProcessed(event.eventId());
        order.verify(acknowledgment).acknowledge();
        verifyNoMoreInteractions(idempotencyService, service, acknowledgment);
    }

    @Test
    void acknowledgesDuplicatesWithoutSavingOrRefreshingTheMarker() {
        var event = event();
        when(idempotencyService.isProcessed(event.eventId())).thenReturn(true);

        consumer.consume(event, acknowledgment);

        verify(acknowledgment).acknowledge();
        verify(idempotencyService, never()).markProcessed(any());
        verifyNoInteractions(service);
    }

    @Test
    void leavesFailedSavesUnmarkedAndUnacknowledgedForRetry() {
        var event = event();
        var failure = new DataAccessResourceFailureException("Database unavailable");
        when(service.save(event)).thenThrow(failure);

        assertThatThrownBy(() -> consumer.consume(event, acknowledgment)).isSameAs(failure);

        verify(idempotencyService, never()).markProcessed(any());
        verifyNoInteractions(acknowledgment);
    }

    @Test
    void doesNotAcknowledgeWhenMarkingTheEventFails() {
        var event = event();
        var failure = new DataAccessResourceFailureException("Redis unavailable");
        doThrow(failure).when(idempotencyService).markProcessed(event.eventId());

        assertThatThrownBy(() -> consumer.consume(event, acknowledgment)).isSameAs(failure);

        verify(service).save(event);
        verifyNoInteractions(acknowledgment);
    }

    @Test
    void propagatesLookupFailuresWithoutSavingOrAcknowledging() {
        var event = event();
        var failure = new DataAccessResourceFailureException("Redis unavailable");
        when(idempotencyService.isProcessed(event.eventId())).thenThrow(failure);

        assertThatThrownBy(() -> consumer.consume(event, acknowledgment)).isSameAs(failure);

        verify(idempotencyService, never()).markProcessed(any());
        verifyNoInteractions(service, acknowledgment);
    }

    @Test
    void propagatesAcknowledgmentFailuresAfterMarkingTheEvent() {
        var event = event();
        var failure = new IllegalStateException("Acknowledgment failed");
        doThrow(failure).when(acknowledgment).acknowledge();

        assertThatThrownBy(() -> consumer.consume(event, acknowledgment)).isSameAs(failure);

        verify(idempotencyService).markProcessed(event.eventId());
    }
}
