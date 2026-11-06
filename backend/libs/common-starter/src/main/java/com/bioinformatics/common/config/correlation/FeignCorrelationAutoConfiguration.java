package com.bioinformatics.common.config.correlation;

import com.bioinformatics.common.correlation.CorrelationContext;
import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
@ConditionalOnProperty(prefix = "common", name = {"enabled", "correlation.enabled"}, havingValue = "true", matchIfMissing = true)
public class FeignCorrelationAutoConfiguration {
    @Bean
    public RequestInterceptor correlationRequestInterceptor() {
        return template -> {
            template.removeHeader(CorrelationContext.HEADER);
            template.header(CorrelationContext.HEADER, CorrelationContext.currentOrNew());
        };
    }
}
