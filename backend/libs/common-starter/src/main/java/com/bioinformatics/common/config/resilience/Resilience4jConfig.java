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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Shared Resilience4j registry configuration used by all microservices.
 *
 * <p>The starter provisions default circuit breaker, retry, and rate limiter registries so service
 * code can opt into standard resilience patterns without repeating boilerplate configuration.
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(CircuitBreakerRegistry.class)
@EnableConfigurationProperties(CommonProperties.class)
@Slf4j
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

        log.info("Creating default CircuitBreaker registry with failure rate {}%, slow call threshold {}% and open-state wait {}ms",
                conf.failureRateThreshold(), conf.slowCallRateThreshold(), conf.waitDurationInOpenStateMs());
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

        log.info("Creating default Retry registry with maxAttempts={} and exponential backoff base {}ms",
                conf.maxAttempts(), conf.waitDurationMs());
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

        log.info("Creating default RateLimiter registry with limitForPeriod={} and refresh period {}ms",
                conf.limitForPeriod(), conf.limitRefreshPeriodMs());
        return RateLimiterRegistry.of(config);
    }
}
