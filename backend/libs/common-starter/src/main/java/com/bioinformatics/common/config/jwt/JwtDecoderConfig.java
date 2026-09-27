package com.bioinformatics.common.config.jwt;

import com.bioinformatics.common.config.CommonProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Configures a {@link JwtDecoder} that validates incoming tokens using the
 * shared HS256 secret distributed by the Config Server.
 *
 * <p>Services that need a different validation strategy (for example RS256 public-key validation)
 * can define their own {@code JwtDecoder} bean; this bean backs off through
 * {@link ConditionalOnMissingBean}.
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(JwtDecoder.class)
@ConditionalOnProperty(prefix = "common.jwt", name = "secret")
@EnableConfigurationProperties(CommonProperties.class)
@Slf4j
public class JwtDecoderConfig {

    private final CommonProperties commonProperties;

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder jwtDecoder() {
        var secret = commonProperties.jwt().secret();
        var secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        log.info("Creating default JWT decoder using HS256 validation for issuer '{}'", commonProperties.jwt().issuer());
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
