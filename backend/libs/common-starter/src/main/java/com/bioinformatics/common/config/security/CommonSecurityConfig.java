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
 * Common {@link SecurityFilterChain} used by every microservice.
 * <ul>
 *   <li>Stateless JWT sessions (no cookies / no CSRF).</li>
 *   <li>Permits actuator health probes (k8s / load-balancer friendly).</li>
 *   <li>Everything else requires a valid Bearer token.</li>
 *   <li>Method-security ({@code @PreAuthorize}) is enabled.</li>
 * </ul>
 * <p>Individual services can override this bean by defining their own
 * {@code SecurityFilterChain} — this configuration backs off via
 * {@link ConditionalOnMissingBean}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnClass(SecurityFilterChain.class)
public class CommonSecurityConfig {

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