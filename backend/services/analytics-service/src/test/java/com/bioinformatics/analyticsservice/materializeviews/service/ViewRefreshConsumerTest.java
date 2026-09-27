package com.bioinformatics.analyticsservice.materializeviews.service;

import com.bioinformatics.shared.models.kafka.events.ViewRefreshRequestedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ViewRefreshConsumerTest {

    @Mock
    private MaterializedViewRefreshService refreshService;

    @Mock
    private RefreshIdempotencyService idempotencyService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private ViewRefreshConsumer consumer;

    @Test
    void skipsDuplicateRefreshEvents() {
        var event = new ViewRefreshRequestedEvent("job-1", "import", Instant.now(), "corr-1");
        when(idempotencyService.isProcessed("job-1")).thenReturn(true);

        consumer.consume(event, acknowledgment);

        verify(acknowledgment).acknowledge();
        verifyNoInteractions(refreshService);
        verify(idempotencyService, never()).markProcessed(anyString());
    }

    @Test
    void refreshesViewsAndMarksJobProcessed() {
        var event = new ViewRefreshRequestedEvent("job-2", "import", Instant.now(), "corr-2");
        when(idempotencyService.isProcessed("job-2")).thenReturn(false);

        consumer.consume(event, acknowledgment);

        verify(refreshService).refreshAllDashboardViews("job-2");
        verify(idempotencyService).markProcessed("job-2");
        verify(acknowledgment).acknowledge();
    }

    @Test
    void propagatesExceptionFromRefreshService() {
        var event = new ViewRefreshRequestedEvent("job-3", "import", Instant.now(), "corr-3");
        when(idempotencyService.isProcessed("job-3")).thenReturn(false);
        doThrow(new RuntimeException("refresh failed")).when(refreshService).refreshAllDashboardViews("job-3");

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> consumer.consume(event, acknowledgment));

        verify(idempotencyService, never()).markProcessed(anyString());
        verify(acknowledgment, never()).acknowledge();
    }
}

