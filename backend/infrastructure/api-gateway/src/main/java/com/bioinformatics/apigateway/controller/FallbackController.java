package com.bioinformatics.apigateway.controller;


import com.bioinformatics.apigateway.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
public class FallbackController {
    // TODO create shared module dto for response and exception handlers

    @RequestMapping("/fallback/dashboard")
    public Mono<ResponseEntity<ErrorResponse>> dashboardFallback(ServerWebExchange exchange) {
        var body = ErrorResponse.builder()
                .error("Dashboard service is temporarily unavailable")
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .path(exchange.getRequest().getPath().value())
                .timestamp(Instant.now())
                .build();

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "60")
                .body(body));
    }
}
