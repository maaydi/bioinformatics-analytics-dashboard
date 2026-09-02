package com.bioinformatics.importservice.service;

import com.bioinformatics.shared.models.kafka.events.ViewRefreshRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

import static com.bioinformatics.shared.models.kafka.KafkaTopics.ANALYTICS_VIEW_REFRESH_REQUESTED;

@Service
@Slf4j
@RequiredArgsConstructor
public class MaterializedViewRefreshService {

    private final KafkaTemplate<String, ViewRefreshRequestedEvent> kafkaTemplate;


    public void refreshAllDashboardViews(String jobId, String source) {
        var event = new ViewRefreshRequestedEvent(
                jobId,
                source,
                Instant.now(),
                UUID.randomUUID().toString()
        );

        log.info("Publishing view refresh request for job <{}> from {}", jobId, source);

        kafkaTemplate.send(ANALYTICS_VIEW_REFRESH_REQUESTED, jobId, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish refresh event for job {}", jobId, ex);
                    } else {
                        log.info("Refresh event published for job {} to partition {} offset {}",
                                jobId,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
