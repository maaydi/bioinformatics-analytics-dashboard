package com.bioinformatics.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Auth-service configuration properties bound from Spring configuration files.
 *
 * <p>These values are used for JWT generation, password policy enforcement,
 * rate limiting, and audit-thread sizing. This record intentionally keeps secret
 * configuration values outside application code logs and avoids embedding secrets
 * in source control.
 */

@ConfigurationProperties(prefix = "app")
public record ApplicationProperties(
        @DefaultValue Jwt jwt,
        @DefaultValue Token token,
        @DefaultValue Password password,
        @DefaultValue RateLimiter rateLimiter,
        @DefaultValue AuditPoolSettings auditPool
) {

    /**
     * JWT configuration block.
     *
     * @param secret                    shared secret used to sign HS256 tokens
     * @param issuer                    JWT issuer claim value
     * @param audience                  JWT audience claim value
     * @param accessTokenExpirySeconds  access token lifetime in seconds
     * @param refreshTokenExpirySeconds refresh token lifetime in seconds
     * @param serviceTokenExpirySeconds backend service token lifetime in seconds
     * @param keyBytesLen               required HMAC key length for HS256
     */
    public record Jwt(String secret, String issuer, String audience, int accessTokenExpirySeconds,
                      int refreshTokenExpirySeconds, int serviceTokenExpirySeconds, int keyBytesLen) {
    }

    /**
     * Session / refresh-token policy.
     *
     * @param refreshRotationEnabled toggles refresh-token rotation on refresh
     * @param maxActiveSessionsPerUser maximum active refresh tokens per user
     */
    public record Token(boolean refreshRotationEnabled, int maxActiveSessionsPerUser) {
    }

    /**
     * Password-strength policy used during change-password operations.
     *
     * @param minLength minimum password length
     * @param requireUppercase require uppercase character
     * @param requireDigit require numeric character
     * @param requireSpecialChar require special character
     * @param bcryptStrength BCrypt strength cost factor
     */
    public record Password(
            int minLength,
            boolean requireUppercase,
            boolean requireDigit,
            boolean requireSpecialChar,
            int bcryptStrength
    ) {
    }

    /**
     * Rate-limiter configuration for login and other high-risk endpoints.
     *
     * @param enabled global switch for rate limiting
     * @param global global rate-limit settings
     * @param endpoints endpoint-specific rate-limit overrides
     */
    public record RateLimiter(
            boolean enabled,
            RateLimiterSettings global,
            List<RateLimiterSettings> endpoints
    ) {
    }

    /**
     * Per-endpoint or global rate-limit settings.
     *
     * @param name logical name of the limiter
     * @param capacity maximum tokens in bucket
     * @param tokens initial tokens in the bucket
     * @param seconds time window in seconds
     */
    public record RateLimiterSettings(
            String name,
            int capacity,
            int tokens,
            int seconds
    ) {
    }

    /**
     * Audit-thread pool configuration for asynchronous security/audit tasks.
     *
     * @param coreSize initial thread count
     * @param maxSize maximum thread count
     * @param queueCapacity queue size for blocked tasks
     * @param threadNamePrefix thread naming prefix
     */
    public record AuditPoolSettings(
            int coreSize,
            int maxSize,
            int queueCapacity,
            String threadNamePrefix
    ) {
    }

}
