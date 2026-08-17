package com.bioinformatics.apigateway.config;


import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver userRouteKeyResolver() {
        return exchange -> {
            var userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            var route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            var routeId = route instanceof Route r ? r.getId() : "unknown";
            return Mono.just((userId != null ? userId : "anonymous") + ":" + routeId);
        };
    }
}

