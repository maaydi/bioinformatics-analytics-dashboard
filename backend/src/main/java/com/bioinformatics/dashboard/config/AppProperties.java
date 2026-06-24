package com.bioinformatics.dashboard.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Batch batch = new Batch();
    private final ImportConfig importConfig = new ImportConfig();
    private final Export export = new Export();
    private final ViewRefresh viewRefresh = new ViewRefresh();
    private final ThreadPoolSettings auditPool = new ThreadPoolSettings();
    private final RateLimiter rateLimiter = new RateLimiter();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long accessTokenExpirySeconds;
        private long refreshTokenExpirySeconds;
    }

    @Getter
    @Setter
    public static class Batch {
        private int chunkSize;
        private int skipLimit;

    }

    @Getter
    @Setter
    public static class ImportConfig {
        private String tempDir;
        private List<String> extensions;
        private ThreadPoolSettings pool;
    }

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
    public static class Export {
        private Csv csv;
    }

    @Getter
    @Setter
    public static class Csv {
        private int maxRows;
    }

    @Getter
    public static class ViewRefresh {
        private int maxAttempts;
        private long perViewTimeoutMs;
        private long retryBackoffMs;
        private long sequenceSlaMs;

        public void setMaxAttempts(int maxAttempts) {
            if (maxAttempts < 1) {
                throw new IllegalStateException("app.view-refresh.max-attempts must be >= 1");
            }
            this.maxAttempts = maxAttempts;
        }

        public void setPerViewTimeoutMs(long perViewTimeoutMs) {
            if (perViewTimeoutMs < 1) {
                throw new IllegalStateException("app.view-refresh.per-view-timeout-ms must be >= 1");
            }
            this.perViewTimeoutMs = perViewTimeoutMs;
        }

        public void setRetryBackoffMs(long retryBackoffMs) {
            if (retryBackoffMs < 0) {
                throw new IllegalStateException("app.view-refresh.retry-backoff-ms must be >= 0");
            }
            this.retryBackoffMs = retryBackoffMs;
        }

        public void setSequenceSlaMs(long sequenceSlaMs) {
            if (sequenceSlaMs < 1) {
                throw new IllegalStateException("app.view-refresh.sequence-sla-ms must be >= 1");
            }
            this.sequenceSlaMs = sequenceSlaMs;
        }
    }

    @Getter
    @Setter
    public static class RateLimiter {
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
