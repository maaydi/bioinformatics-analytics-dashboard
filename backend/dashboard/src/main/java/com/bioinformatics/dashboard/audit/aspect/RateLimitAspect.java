package com.bioinformatics.dashboard.audit.aspect;

import com.bioinformatics.common.exception.RateLimitExceededException;
import com.bioinformatics.dashboard.audit.annotation.RateLimited;
import com.bioinformatics.dashboard.config.AppProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AOP aspect that enforces per-endpoint request throttling for the dashboard API.
 *
 * <p>The limiter is configured from {@link AppProperties} and is keyed by endpoint type and client IP. It prevents
 * abusive access patterns by consuming tokens from a Bucket before allowing the annotated method invocation.</p>
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitAspect {
    private final AppProperties appProperties;

    private Map<String, AppProperties.RateLimiterSettings> rateLimiters;

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    /**
     * Initializes the configured rate-limit definitions after the bean is created.
     */
    @PostConstruct
    public void init() {
        log.info("[DASHBOARD][RATE_LIMIT] Initializing rate limiter configuration");
        rateLimiters = initRateLimiters(appProperties);
    }

    private static Map<String, AppProperties.RateLimiterSettings> initRateLimiters(AppProperties appProperties) {
        if (appProperties == null || appProperties.getRateLimiter() == null) {
            throw new IllegalStateException("Rate limiter configuration is missing in application properties.");
        }
        var limiters = new ArrayList<AppProperties.RateLimiterSettings>();
        limiters.add(appProperties.getRateLimiter().getGlobal());
        limiters.addAll(appProperties.getRateLimiter().getEndpoints());
        return limiters.stream()
                .collect(ConcurrentHashMap::new, (map, limiter) -> map.put(limiter.getName(), limiter),
                        ConcurrentHashMap::putAll);
    }

    private Bucket createNewBucket(String configKey) {
        var config = rateLimiters.get(configKey);
        if (config == null) {
            throw new IllegalArgumentException("No rate limit configuration found with name: " + configKey);
        }

        var limit = Bandwidth.builder()
                .capacity(config.getCapacity())
                .refillGreedy(config.getTokens(), Duration.ofSeconds(config.getSeconds()))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Enforces the rate limit for any method annotated with {@link RateLimited}.
     *
     * @param joinPoint the proceeding join point
     * @return the original method result when permitted
     * @throws Throwable when the underlying method fails
     */
    @Around("@annotation(com.bioinformatics.dashboard.audit.annotation.RateLimited)")
    public Object rateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!appProperties.getRateLimiter().isEnabled()) {
            log.debug("[DASHBOARD][RATE_LIMIT] Rate limiting disabled for request to {}", joinPoint.getSignature().getName());
            return joinPoint.proceed();
        }
        var rateLimited = getAnnotation(joinPoint);
        if (rateLimited != null) {
            var configKey = rateLimited.key();
            var request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            var clientIp = request.getRemoteAddr();
            var bucketId = configKey + "-" + clientIp;
            var bucket = cache.computeIfAbsent(bucketId, _ -> createNewBucket(configKey));
            if (bucket.tryConsume(1)) {
                return joinPoint.proceed();
            } else {
                log.warn("[DASHBOARD][RATE_LIMIT] Rate limit exceeded for endpoint={} clientIp={}", configKey, clientIp);
                throw new RateLimitExceededException("Rate limit exceeded. Try again later.");
            }
        }
        log.warn("[DASHBOARD][RATE_LIMIT] RateLimit annotation is not enabled for this method {}", joinPoint.getSignature().getName());
        return joinPoint.proceed();
    }

    private RateLimited getAnnotation(JoinPoint joinPoint) {
        try {
            var signature = (MethodSignature) joinPoint.getSignature();
            var target = joinPoint.getTarget();

            var method = target.getClass().getMethod(signature.getName(), signature.getParameterTypes());

            return method.getAnnotation(RateLimited.class);
        } catch (Exception e) {
            log.warn("[DASHBOARD][RATE_LIMIT] Could not extract @RateLimited annotation for method={} error={}", joinPoint.getSignature().getName(), e.getMessage());
            return null;
        }
    }
}
