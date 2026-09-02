package com.bioinformatics.shared.models.kafka.events;

import java.time.Instant;

public record ViewRefreshRequestedEvent(
        String jobId, String source, Instant jobCompletedAt, String correlationId
) {
}
