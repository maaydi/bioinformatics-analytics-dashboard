package com.bioinformatics.common.config.web;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Provides two {@link WebClient.Builder} beans:
 * <ul>
 *   <li>{@code loadBalancedWebClientBuilder} — resolves service names via
 *       Eureka (e.g. {@code http://gene-service/api/genes}).</li>
 *   <li>{@code webClientBuilder} — plain builder for external URLs.</li>
 * </ul>
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