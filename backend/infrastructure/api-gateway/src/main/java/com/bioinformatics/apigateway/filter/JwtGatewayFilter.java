package com.bioinformatics.apigateway.filter;


import com.bioinformatics.apigateway.config.ApplicationProperties;
import com.bioinformatics.shared.models.security.AppClaims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static com.bioinformatics.shared.models.security.AppHeaders.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtGatewayFilter implements GlobalFilter, Ordered {
    private static final String BEARER = "Bearer ";


    private final ApplicationProperties properties;

    @Override
    public @NonNull Mono<Void> filter(ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        if (shouldNotFilter(exchange.getRequest())) {
            return chain.filter(exchange);
        }

        var authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER)) {
            return unauthorized(exchange);
        }

        var token = authHeader.substring(BEARER.length());
        try {
            var claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (claims.getExpiration() != null
                    && claims.getExpiration().before(new Date())) {
                return unauthorized(exchange);
            }

            var userId = claims.getSubject();
            var role = getRoleClaim(claims.get(AppClaims.ROLE.getClaim()));
            var mutated = exchange.getRequest().mutate()
                    .header(USER_ID.getHeader(), Objects.requireNonNullElse(userId, USER_ID.getDefaultValue()))
                    .header(USER_ROLE.getHeader(), role.toArray(String[]::new))
                    // TODO change this logic for provider
                    .header(DATA_PROVIDER.getHeader(), Objects.requireNonNullElse(null, DATA_PROVIDER.getDefaultValue()))
                    .build();

            return chain.filter(exchange.mutate().request(mutated).build());

        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT invalid: {}", e.getMessage());
            return unauthorized(exchange);
        }
    }

    private boolean shouldNotFilter(final ServerHttpRequest request) {
        var path = request.getURI().getPath();
        return HttpMethod.POST.matches(request.getMethod().name())
                && properties.security().publicEndpoints().stream().anyMatch(path::contains);
    }

    private SecretKey getSigningKey() {
        var hs256KeyBytes = properties.jwt().keyBytesLen();
        byte[] keyBytes = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < hs256KeyBytes) {
            byte[] padded = new byte[hs256KeyBytes];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private List<String> getRoleClaim(Object role) {
        if (role instanceof List<?> roles) {
            return roles.stream().map(Object::toString).toList();
        }
        return List.of(USER_ROLE.getDefaultValue());
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add(HttpHeaders.WWW_AUTHENTICATE,
                BEARER.concat("error=\"invalid_token\""));
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

}
