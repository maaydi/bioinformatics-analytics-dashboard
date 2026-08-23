package com.bioinformatics.apigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record ApplicationProperties(
        @DefaultValue Jwt jwt,
        @DefaultValue Security security) {

    public record Jwt(String secret, String issuer, String audience, int accessTokenExpirySeconds,
                      int refreshTokenExpirySeconds, int serviceTokenExpirySeconds, int keyBytesLen) {
    }

    public record Security(List<String> publicEndpoints) {
    }

}
