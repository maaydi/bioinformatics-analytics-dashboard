package com.bioinformatics.apigateway.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import static com.bioinformatics.shared.models.security.Constants.USER_ID_HEADER;

@Configuration
@Slf4j
public class RateLimitConfig {

    @Bean
    public KeyResolver userRouteKeyResolver() {
        return exchange -> {
            var userId = exchange.getRequest().getHeaders().getFirst(USER_ID_HEADER);
            var route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            var routeId = route instanceof Route r ? r.getId() : "unknown";
            var key = (userId != null ? userId : "anonymous") + ":" + routeId;
            log.debug("Resolved rate limit key='{}' for path={}", key, exchange.getRequest().getURI().getPath());
            return Mono.just(key);
        };
    }
}

