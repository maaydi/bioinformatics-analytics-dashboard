package com.bioinformatics.apigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record ApplicationProperties(
        @DefaultValue Jwt jwt,
        @DefaultValue Security security) {

    public record Jwt(String secret) {
    }

    public record Security(List<String> publicEndpoints) {
    }

}
