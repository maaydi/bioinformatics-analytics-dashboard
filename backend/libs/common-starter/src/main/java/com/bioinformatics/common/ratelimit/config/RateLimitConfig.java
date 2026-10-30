package com.bioinformatics.common.ratelimit.config;

import com.bioinformatics.common.ratelimit.aspect.RateLimitAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "app.rate-limiter", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(RateLimitProperties.class)
@EnableAspectJAutoProxy
public class RateLimitConfig {

    @Bean
    public RateLimitAspect rateLimitAspect(RateLimitProperties properties) {
        return new RateLimitAspect(properties);
    }
}
