package com.bioinformatics.common.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.Assert;

@ConfigurationProperties(prefix = "app.audit-pool")
public record AuditPublisherProperties(
        @DefaultValue("1") int coreSize,
        @DefaultValue("10") int maxSize,
        @DefaultValue("25") int queueCapacity,
        @DefaultValue("audit-") String threadNamePrefix) {

    public AuditPublisherProperties {
        Assert.isTrue(coreSize > 0, "app.audit-pool.core-size must be positive");
        Assert.isTrue(maxSize >= coreSize, "app.audit-pool.max-size must be at least core-size");
        Assert.isTrue(queueCapacity > 0, "app.audit-pool.queue-capacity must be positive");
        Assert.hasText(threadNamePrefix, "app.audit-pool.thread-name-prefix must not be blank");
    }
}
