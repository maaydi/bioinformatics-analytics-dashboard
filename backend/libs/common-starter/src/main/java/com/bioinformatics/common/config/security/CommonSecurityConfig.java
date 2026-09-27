package com.bioinformatics.common.config.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Default security baseline for all microservices in the platform.
 *
 * <p>When a service does not define its own {@link SecurityFilterChain}, this configuration enables
 * stateless JWT authentication, guards all non-public endpoints, and leaves metrics/health endpoints
 * accessible for operational checks and load-balancer probes.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnClass(SecurityFilterChain.class)
public class CommonSecurityConfig {

    /**
     * Builds the default stateless security chain for microservices.
     *
     * @param http       HTTP security configuration
     * @param jwtDecoder decoder used to validate bearer tokens
     * @return default security filter chain
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    @ConditionalOnBean(JwtDecoder.class)
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET,
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/actuator/prometheus").permitAll()
                        .requestMatchers("/actuator/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(CustomJwtAuthenticationConverter.jwtAuthenticationConverter())
                        )
                );

        return http.build();
    }
}