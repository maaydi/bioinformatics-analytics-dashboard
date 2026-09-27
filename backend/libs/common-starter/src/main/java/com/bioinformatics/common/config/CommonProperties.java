package com.bioinformatics.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Centralized configuration properties for the common starter.
 *
 * <p>All keys live under the {@code common} prefix and can be overridden by individual services
 * via the config server or environment-specific configuration files.
 *
 * <p>These properties cover shared JWT, datasource, Kafka, resilience, tracing, caching, and UniProt API settings.
 * They enable services to share a common operational baseline without repeating boilerplate configuration.
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
        @DefaultValue Cache cache,
        @DefaultValue UniprotApi uniprotApi
) {

    /**
     * JWT-related shared settings for services using the common starter.
     *
     * @param secret                   shared HS256 secret
     * @param issuer                   expected JWT issuer claim
     * @param accessTokenExpirySeconds access-token lifetime in seconds
     */
    public record Jwt(
            // Shared HS256 secret (injected from Config Server / Vault).
            String secret,
            // Expected token issuer claim.
            @DefaultValue("bioinformatics-auth") String issuer,
            // Access-token TTL in seconds (used only when generating tokens).
            @DefaultValue("3600") long accessTokenExpirySeconds
    ) {
    }

    /**
     * Shared datasource configuration for primary and replica databases.
     *
     * @param primaryUrl primary database URL
     * @param primaryUsername primary database username
     * @param primaryPassword primary database password
     * @param replicaUrl replica database URL
     * @param replicaUsername replica username
     * @param replicaPassword replica password
     * @param driverClassName JDBC driver class name
     * @param pool datasource pool settings
     */
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
        /**
         * Connection pool configuration.
         *
         * @param maxSize max number of connections
         * @param minIdle minimum idle connections
         * @param connectionTimeoutMs connection timeout in milliseconds
         */
        public record Pool(
                @DefaultValue("10") int maxSize,
                @DefaultValue("5") int minIdle,
                @DefaultValue("30000") long connectionTimeoutMs
        ) {
        }
    }

    /**
     * Kafka connection and client settings used by shared infrastructure-
     * integration components.
     *
     * @param bootstrapServers Kafka bootstrap brokers
     * @param producer producer settings
     * @param consumer consumer settings
     */
    public record Kafka(
            @DefaultValue("localhost:9092") String bootstrapServers,
            @DefaultValue Producer producer,
            @DefaultValue Consumer consumer
    ) {
        /**
         * Producer-specific Kafka values.
         *
         * @param acks producer acknowledgements
         * @param retries producer retries
         * @param batchSize producer batch size
         * @param lingerMs producer linger value
         */
        public record Producer(
                @DefaultValue("all") String acks,
                @DefaultValue("3") int retries,
                @DefaultValue("16384") int batchSize,
                @DefaultValue("5") int lingerMs
        ) {
        }

        /**
         * Consumer-specific Kafka values.
         *
         * @param groupId consumer group id
         * @param autoOffsetReset offset reset policy
         * @param concurrency concurrency level
         * @param batchListener whether batch listeners are enabled
         * @param ackMode acknowledgement mode
         */
        public record Consumer(
                String groupId,
                @DefaultValue("earliest") String autoOffsetReset,
                @DefaultValue("3") int concurrency,
                @DefaultValue("false") boolean batchListener,
                @DefaultValue("manual") String ackMode
        ) {
        }
    }

    /**
     * Resilience4j circuit breaker / retry / rate-limit settings.
     *
     * @param circuitBreaker circuit breaker config
     * @param retry retry config
     * @param rateLimiter rate limiter config
     */
    public record Resilience4j(
            @DefaultValue CircuitBreaker circuitBreaker,
            @DefaultValue Retry retry,
            @DefaultValue RateLimiter rateLimiter
    ) {
        /**
         * Circuit breaker settings.
         *
         * @param name circuit breaker name
         * @param failureRateThreshold failure threshold percentage
         * @param waitDurationInOpenStateMs time to remain open
         * @param permittedNumberOfCallsInHalfOpenState allowed half-open calls
         * @param slidingWindowSize sliding window size
         * @param slowCallRateThreshold slow call threshold percentage
         * @param slowCallDurationThresholdMs slow-call threshold
         */
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

        /**
         * Retry policy settings.
         *
         * @param name retry name
         * @param maxAttempts maximum retries
         * @param waitDurationMs wait duration per attempt
         * @param exponentialBackoffMultiplier backoff multiplier
         */
        public record Retry(
                @DefaultValue("default") String name,
                @DefaultValue("3") int maxAttempts,
                @DefaultValue("1000") long waitDurationMs,
                @DefaultValue("2.0") double exponentialBackoffMultiplier
        ) {
        }

        /**
         * Rate limiter settings.
         *
         * @param name limiter name
         * @param limitForPeriod requests allowed per period
         * @param limitRefreshPeriodMs period length in milliseconds
         * @param timeoutDurationMs timeout when request is blocked
         */
        public record RateLimiter(
                @DefaultValue("default") String name,
                @DefaultValue("100") int limitForPeriod,
                @DefaultValue("1000") long limitRefreshPeriodMs,
                @DefaultValue("0") long timeoutDurationMs
        ) {
        }
    }

    /**
     * Distributed tracing configuration.
     *
     * @param samplingRate sampling fraction
     * @param zipkinEndpoint Zipkin collector endpoint
     * @param propagation trace propagation format
     */
    public record Tracing(
            @DefaultValue("1.0") float samplingRate,
            @DefaultValue("http://localhost:9411/api/v2/spans") String zipkinEndpoint,
            @DefaultValue("b3") String propagation
    ) {
    }

    /**
     * Shared cache configuration.
     *
     * @param enabled enable cache support
     * @param allowedBasePackages packages that are allowed to be cached
     * @param entryTtlDuration default TTL duration
     */
    public record Cache(@DefaultValue("true") boolean enabled,
                        @DefaultValue("com.bioinformatics,java.util") List<String> allowedBasePackages,
                        @DefaultValue("PT6H") String entryTtlDuration) {
    }

    /**
     * UniProt API settings used by shared clients and batch readers.
     *
     * @param baseUrl UniProt REST base URL
     * @param batch batch query settings
     * @param readTimeoutDuration default read timeout duration
     * @param httpClient HTTP client settings
     */
    public record UniprotApi(
            @DefaultValue("https://rest.uniprot.org") String baseUrl,
            @DefaultValue Batch batch,
            @DefaultValue("PT1H") String readTimeoutDuration,
            @DefaultValue HttpClientConfig httpClient
    ) {
    }

    /**
     * Batch-related config used by UniProt readers.
     *
     * @param chunkSize number of records per batch
     * @param skipLimit skip threshold for batch cursor movement
     */
    public record Batch(@DefaultValue("100") int chunkSize, @DefaultValue("1000") int skipLimit) {
    }

    /**
     * HTTP client configuration.
     *
     * @param timeoutDuration default client timeout duration
     */
    public record HttpClientConfig(@DefaultValue("PT10S") String timeoutDuration) {
    }
}