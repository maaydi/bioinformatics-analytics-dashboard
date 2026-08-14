package com.bioinformatics.common.config.resilience;


import com.bioinformatics.common.config.CommonProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Provides default Resilience registries with sensible microservice defaults.
 * <p>Services can retrieve named instances from the registries and annotate
 * methods with {@code @CircuitBreaker(name = "...")}, {@code @Retry}, etc.
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(CircuitBreakerRegistry.class)
@EnableConfigurationProperties(CommonProperties.class)
public class Resilience4jConfig {

    private final CommonProperties commonProperties;

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        var conf = commonProperties.resilience4j().circuitBreaker();
        var config = CircuitBreakerConfig.custom()
                .failureRateThreshold(conf.failureRateThreshold())
                .slowCallRateThreshold(conf.slowCallRateThreshold())
                .slowCallDurationThreshold(Duration.ofMillis(conf.slowCallDurationThresholdMs()))
                .waitDurationInOpenState(Duration.ofMillis(conf.waitDurationInOpenStateMs()))
                .permittedNumberOfCallsInHalfOpenState(conf.permittedNumberOfCallsInHalfOpenState())
                .slidingWindowSize(conf.slidingWindowSize())
                .build();
        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    public RetryRegistry retryRegistry() {
        var conf = commonProperties.resilience4j().retry();
        var config = RetryConfig.custom()
                .maxAttempts(conf.maxAttempts())
                .intervalFunction(IntervalFunction.ofExponentialBackoff(
                        Duration.ofMillis(conf.waitDurationMs()),
                        conf.exponentialBackoffMultiplier()
                )).build();
        return RetryRegistry.of(config);
    }

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        var conf = commonProperties.resilience4j().rateLimiter();
        var config = RateLimiterConfig.custom()
                .limitForPeriod(conf.limitForPeriod())
                .limitRefreshPeriod(Duration.ofMillis(conf.limitRefreshPeriodMs()))
                .timeoutDuration(Duration.ofMillis(conf.timeoutDurationMs()))
                .build();
        return RateLimiterRegistry.of(config);
    }
}
