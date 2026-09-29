package com.bioinformatics.common.config.tracing;

import brave.Tracing;
import brave.propagation.B3Propagation;
import brave.sampler.Sampler;
import com.bioinformatics.common.config.CommonProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.urlconnection.URLConnectionSender;

/**
 * Common Brave tracing configuration used by all backend services.
 *
 * <p>The starter creates a single {@link Tracing} bean that bridges into Spring Boot's Micrometer
 * tracing integration and exports spans to Zipkin using B3 propagation headers.
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(Tracing.class)
@EnableConfigurationProperties(CommonProperties.class)
@Slf4j
public class TracingConfig {

    private final CommonProperties commonProperties;

    @Bean
    @ConditionalOnMissingBean(Tracing.class)
    public Tracing tracing(
            @Value("${spring.application.name:unknown-service}") String serviceName) {

        var tracingProps = commonProperties.tracing();
        var sender = URLConnectionSender.create(tracingProps.zipkinEndpoint());
        var spanHandler = AsyncZipkinSpanHandler.create((BytesMessageSender) sender);

        log.info("Configuring Brave tracing for service '{}' with Zipkin endpoint '{}' and sampling rate {}",
                serviceName, tracingProps.zipkinEndpoint(), tracingProps.samplingRate());

        var builder = Tracing.newBuilder()
                .localServiceName(serviceName)
                .sampler(Sampler.create(tracingProps.samplingRate()))
                .addSpanHandler(spanHandler);

        if ("b3".equalsIgnoreCase(tracingProps.propagation())) {
            builder.propagationFactory(B3Propagation.FACTORY);
        }

        return builder.build();
    }
}
