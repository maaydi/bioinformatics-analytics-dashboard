package com.bioinformatics.exportservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app")
public record ApplicationProperties(@DefaultValue Export export) {
    public record Export(@DefaultValue("100000") int maxRows, @DefaultValue String tempDir,
                         @DefaultValue ThreadPoolSettings pool, @DefaultValue Batch batch) {
    }

    public record ThreadPoolSettings(int coreSize,
                                     int maxSize,
                                     int queueCapacity,
                                     String threadNamePrefix) {

    }

    public record Batch(@DefaultValue("500") int chunkSize) {
    }
}
