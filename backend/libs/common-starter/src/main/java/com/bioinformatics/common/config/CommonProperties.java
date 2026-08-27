package com.bioinformatics.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Centralised configuration properties for the common starter.
 * All keys live under the {@code common} prefix and are overridable
 * per-service via the Config Server.
 */
@ConfigurationProperties(prefix = "common")
public record CommonProperties(
        // Master switch — set to {@code false} to disable the starter entirely.
        @DefaultValue("true") boolean enabled,

        @DefaultValue Jwt jwt,
        @DefaultValue DataSource datasource,
        @DefaultValue Kafka kafka,
        @DefaultValue Resilience4j resilience4j,
        @DefaultValue Tracing tracing,
        @DefaultValue Cache cache
) {

    public record Jwt(
            // Shared HS256 secret (injected from Config Server / Vault).
            String secret,
            // Expected token issuer claim.
            @DefaultValue("bioinformatics-auth") String issuer,
            // Access-token TTL in seconds (used only when generating tokens).
            @DefaultValue("3600") long accessTokenExpirySeconds
    ) {
    }

    public record DataSource(
            String primaryUrl,
            String primaryUsername,
            String primaryPassword,
            String replicaUrl,
            String replicaUsername,
            String replicaPassword,
            @DefaultValue("org.postgresql.Driver") String driverClassName,
            @DefaultValue Pool pool
    ) {
        public record Pool(
                @DefaultValue("10") int maxSize,
                @DefaultValue("5") int minIdle,
                @DefaultValue("30000") long connectionTimeoutMs
        ) {
        }
    }

    public record Kafka(
            @DefaultValue("localhost:9092") String bootstrapServers,
            @DefaultValue Producer producer,
            @DefaultValue Consumer consumer
    ) {
        public record Producer(
                @DefaultValue("all") String acks,
                @DefaultValue("3") int retries,
                @DefaultValue("16384") int batchSize,
                @DefaultValue("5") int lingerMs
        ) {
        }

        public record Consumer(
                // Mandatory — each service must set its own group id.
                String groupId,
                @DefaultValue("earliest") String autoOffsetReset,
                @DefaultValue("3") int concurrency,
                @DefaultValue("false") boolean batchListener
        ) {
        }
    }

    public record Resilience4j(
            @DefaultValue CircuitBreaker circuitBreaker,
            @DefaultValue Retry retry,
            @DefaultValue RateLimiter rateLimiter
    ) {
        public record CircuitBreaker(
                @DefaultValue("default") String name,
                @DefaultValue("50.0") float failureRateThreshold,
                @DefaultValue("10000") int waitDurationInOpenStateMs,
                @DefaultValue("3") int permittedNumberOfCallsInHalfOpenState,
                @DefaultValue("10") int slidingWindowSize,
                @DefaultValue("80") int slowCallRateThreshold,
                @DefaultValue("2000") long slowCallDurationThresholdMs
        ) {
        }

        public record Retry(
                @DefaultValue("default") String name,
                @DefaultValue("3") int maxAttempts,
                @DefaultValue("1000") long waitDurationMs,
                @DefaultValue("2.0") double exponentialBackoffMultiplier
        ) {
        }

        public record RateLimiter(
                @DefaultValue("default") String name,
                @DefaultValue("100") int limitForPeriod,
                @DefaultValue("1000") long limitRefreshPeriodMs,
                @DefaultValue("0") long timeoutDurationMs
        ) {
        }
    }

    public record Tracing(
            @DefaultValue("1.0") float samplingRate,
            @DefaultValue("http://localhost:9411/api/v2/spans") String zipkinEndpoint,
            @DefaultValue("b3") String propagation
    ) {
    }

    public record Cache(@DefaultValue("true") boolean enabled,
                        @DefaultValue("com.bioinformatics,java.util") List<String> allowedBasePackages,
                        @DefaultValue("PT6H") String entryTtlDuration) {
    }
}