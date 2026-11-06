package com.bioinformatics.common.config.correlation;

import com.bioinformatics.common.correlation.CorrelationFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(name = {"jakarta.servlet.Filter", "org.springframework.web.filter.OncePerRequestFilter"})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "common", name = {"enabled", "correlation.enabled"}, havingValue = "true", matchIfMissing = true)
public class CorrelationAutoConfiguration {
    @Bean
    public CorrelationFilter correlationFilter() {
        return new CorrelationFilter();
    }
}
