package com.bioinformatics.analyticsservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app")
public record ApplicationProperties(@DefaultValue ViewRefresh viewRefresh) {

    public record ViewRefresh(
            int maxAttempts,
            long perViewTimeoutMs,
            long retryBackoffMs,
            long sequenceSlaMs
    ) {

        public ViewRefresh {
            if (maxAttempts < 1) {
                throw new IllegalStateException(
                        "app.view-refresh.max-attempts must be >= 1"
                );
            }

            if (perViewTimeoutMs < 1) {
                throw new IllegalStateException(
                        "app.view-refresh.per-view-timeout-ms must be >= 1"
                );
            }

            if (retryBackoffMs < 0) {
                throw new IllegalStateException(
                        "app.view-refresh.retry-backoff-ms must be >= 0"
                );
            }

            if (sequenceSlaMs < 1) {
                throw new IllegalStateException(
                        "app.view-refresh.sequence-sla-ms must be >= 1"
                );
            }
        }
    }
}
