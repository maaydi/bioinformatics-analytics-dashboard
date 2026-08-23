package com.bioinformatics.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record ApplicationProperties(
        @DefaultValue Jwt jwt,
        @DefaultValue Token token,
        @DefaultValue Password password,
        @DefaultValue RateLimiter rateLimiter,
        @DefaultValue AuditPoolSettings auditPool
) {

    public record Jwt(String secret, String issuer, String audience, int accessTokenExpirySeconds,
                      int refreshTokenExpirySeconds, int serviceTokenExpirySeconds, int keyBytesLen) {
    }

    public record Token(boolean refreshRotationEnabled, int maxActiveSessionsPerUser) {
    }

    public record Password(
            int minLength,
            boolean requireUppercase,
            boolean requireDigit,
            boolean requireSpecialChar,
            int bcryptStrength
    ) {
    }

    public record RateLimiter(
            boolean enabled,
            RateLimiterSettings global,
            List<RateLimiterSettings> endpoints
    ) {
    }

    public record RateLimiterSettings(
            String name,
            int capacity,
            int tokens,
            int seconds
    ) {
    }

    public record AuditPoolSettings(
            int coreSize,
            int maxSize,
            int queueCapacity,
            String threadNamePrefix
    ) {
    }

}
