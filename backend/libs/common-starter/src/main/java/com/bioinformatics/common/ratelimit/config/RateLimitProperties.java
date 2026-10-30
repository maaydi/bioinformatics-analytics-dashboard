package com.bioinformatics.common.ratelimit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@ConfigurationProperties(prefix = "app.rate-limiter")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue Settings global,
        List<Settings> endpoints) {

    public RateLimitProperties {
        Objects.requireNonNull(global, "Global rate limit settings are required");
        if (!"global".equals(global.name())) {
            throw new IllegalArgumentException("The default rate limit policy must be named global");
        }
        endpoints = endpoints == null ? List.of() : List.copyOf(endpoints);
        var names = new HashSet<String>();
        names.add(global.name());
        for (var settings : endpoints) {
            if (!names.add(settings.name())) {
                throw new IllegalArgumentException("Duplicate rate limit policy: " + settings.name());
            }
        }
    }

    public record Settings(
            @DefaultValue("global") String name,
            @DefaultValue("100") long capacity,
            @DefaultValue("100") long tokens,
            @DefaultValue("60") long seconds) {

        public Settings {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Rate limit policy name must not be blank");
            }
            if (capacity <= 0 || tokens <= 0 || seconds <= 0) {
                throw new IllegalArgumentException("Rate limit capacity, tokens and seconds must be positive for " + name);
            }
        }
    }
}
