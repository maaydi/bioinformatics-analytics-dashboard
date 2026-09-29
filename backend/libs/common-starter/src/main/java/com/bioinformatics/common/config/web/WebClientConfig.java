package com.bioinformatics.common.config.web;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Provides shared {@link WebClient.Builder} beans for the platform.
 *
 * <p>The load-balanced builder is intended for intra-service HTTP calls resolved through the service
 * registry, while the primary builder is kept as the default choice for external HTTP or custom URI
 * targets that do not require discovery.
 */
@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    @Primary
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}