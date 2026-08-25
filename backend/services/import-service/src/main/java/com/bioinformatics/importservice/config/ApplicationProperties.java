package com.bioinformatics.importservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record ApplicationProperties(ImportConfig importConfig, UniProtApi uniprotApi, Batch batch) {

    public record ImportConfig(String tempDir,
                               List<String> extensions,
                               ThreadPoolSettings pool) {
    }

    public record ThreadPoolSettings(int coreSize,
                                     int maxSize,
                                     int queueCapacity,
                                     String threadNamePrefix) {

    }

    public record UniProtApi(String baseUrl, Batch batch) {
    }

    public record Batch(int chunkSize, int skipLimit) {
    }
}
