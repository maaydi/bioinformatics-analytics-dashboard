package com.bioinformatics.dashboard.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Central application properties for the dashboard module.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {
    private ThreadPoolSettings auditPool = new ThreadPoolSettings();
    private RateLimiter rateLimiter = new RateLimiter();

    @Getter
    @Setter
    public static class ThreadPoolSettings {
        private int coreSize;
        private int maxSize;
        private int queueCapacity;
        private String threadNamePrefix;
    }

    @Getter
    @Setter
    public static class RateLimiter {
        private boolean enabled;
        private RateLimiterSettings global;
        private List<RateLimiterSettings> endpoints;
    }

    @Getter
    @Setter
    public static class RateLimiterSettings {
        private String name;
        private int capacity;
        private int tokens;
        private int seconds;
    }
}
