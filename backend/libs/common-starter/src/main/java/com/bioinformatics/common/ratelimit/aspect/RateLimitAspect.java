package com.bioinformatics.common.ratelimit.aspect;

import com.bioinformatics.common.exception.RateLimitExceededException;
import com.bioinformatics.common.ratelimit.annotation.RateLimited;
import com.bioinformatics.common.ratelimit.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces a shared budget per policy and client IP, local to each service instance.
 */
@Aspect
@Order(1)
@Slf4j
public class RateLimitAspect {

    private final Map<String, RateLimitProperties.Settings> policies;
    private final Map<BucketKey, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitAspect(RateLimitProperties properties) {
        var settings = new HashMap<String, RateLimitProperties.Settings>();
        settings.put(properties.global().name(), properties.global());
        properties.endpoints().forEach(policy -> settings.put(policy.name(), policy));
        policies = Map.copyOf(settings);
    }

    @Around("@annotation(rateLimited)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        var policy = policies.get(rateLimited.key());
        if (policy == null) {
            throw new IllegalStateException("No rate limit policy configured for: " + rateLimited.key());
        }
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            throw new IllegalStateException("@RateLimited requires a servlet request");
        }
        var clientIp = attributes.getRequest().getRemoteAddr();
        var key = new BucketKey(rateLimited.key(), clientIp);
        var bucket = buckets.computeIfAbsent(key, _ -> createBucket(policy));
        if (!bucket.tryConsume(1)) {
            log.warn("[RATE_LIMIT] Rate limit exceeded for policy={} clientIp={}", rateLimited.key(), clientIp);
            throw new RateLimitExceededException("Rate limit exceeded. Try again later.");
        }
        return joinPoint.proceed();
    }

    private static Bucket createBucket(RateLimitProperties.Settings policy) {
        var bandwidth = Bandwidth.builder()
                .capacity(policy.capacity())
                .refillGreedy(policy.tokens(), Duration.ofSeconds(policy.seconds()))
                .build();
        return Bucket.builder().addLimit(bandwidth).build();
    }

    private record BucketKey(String policy, String clientIp) {
    }
}
