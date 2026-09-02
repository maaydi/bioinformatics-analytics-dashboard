package com.bioinformatics.analyticsservice.materializeviews.service;

import com.bioinformatics.shared.models.kafka.events.ViewRefreshRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import static com.bioinformatics.shared.models.kafka.KafkaGroupId.ANALYTICS_VIEW_REFRESHERS;
import static com.bioinformatics.shared.models.kafka.KafkaTopics.ANALYTICS_VIEW_REFRESH_REQUESTED;

@Component
@RequiredArgsConstructor
@Slf4j
public class ViewRefreshConsumer {

    private final MaterializedViewRefreshService refreshService;
    private final RefreshIdempotencyService idempotencyService;

    @KafkaListener(
            topics = ANALYTICS_VIEW_REFRESH_REQUESTED,
            groupId = ANALYTICS_VIEW_REFRESHERS,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(@Payload ViewRefreshRequestedEvent event, Acknowledgment ack) {
        if (idempotencyService.isProcessed(event.jobId())) {
            log.warn("Duplicate event for job {}, skipping", event.jobId());
            ack.acknowledge();
            return;
        }
        log.info("Received refresh request for import uniprot data from {} job {} (correlationId: {})", event.source(),
                event.jobId(), event.correlationId());

        try {
            refreshService.refreshAllDashboardViews(event.jobId());
            idempotencyService.markProcessed(event.jobId());
            ack.acknowledge();
            log.info("Successfully processed refresh for job {}", event.jobId());
        } catch (Exception e) {
            log.error("Critical failure refreshing views for job {}", event.jobId(), e);
            throw e;
        }
    }
}
