package com.bioinformatics.common.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.audit-pool")
public record AuditPublisherProperties(
        @DefaultValue("1") int coreSize,
        @DefaultValue("10") int maxSize,
        @DefaultValue("25") int queueCapacity,
        @DefaultValue("audit-") String threadNamePrefix) {
}
