package com.bioinformatics.importservice.service;

import com.bioinformatics.common.correlation.CorrelationContext;
import com.bioinformatics.shared.models.kafka.events.ViewRefreshRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

import static com.bioinformatics.shared.models.kafka.KafkaTopics.PROTEIN_EVENTS_IMPORTED;

@Service
@Slf4j
@RequiredArgsConstructor
public class MaterializedViewRefreshService {

    private final KafkaTemplate<String, Object> kafkaTemplate;


    public void refreshAllDashboardViews(String jobId, String source) {
        var event = new ViewRefreshRequestedEvent(
                jobId,
                source,
                Instant.now(),
                CorrelationContext.currentOrNew()
        );

        try (var ignored = CorrelationContext.open(event.correlationId())) {
            log.info("Publishing view refresh request for job <{}> from {}", jobId, source);
            kafkaTemplate.send(PROTEIN_EVENTS_IMPORTED, jobId, event)
                    .whenComplete((result, ex) -> {
                        try (var callbackScope = CorrelationContext.open(event.correlationId())) {
                            if (ex != null) {
                                log.error("Failed to publish refresh event for job {}", jobId, ex);
                            } else {
                                log.info("Refresh event published for job {} to partition {} offset {}",
                                        jobId,
                                        result.getRecordMetadata().partition(),
                                        result.getRecordMetadata().offset());
                            }
                        }
                    });
        }
    }
}
