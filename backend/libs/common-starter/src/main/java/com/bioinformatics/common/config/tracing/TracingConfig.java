package com.bioinformatics.common.config.tracing;


import brave.Tracing;
import brave.propagation.B3Propagation;
import brave.sampler.Sampler;
import com.bioinformatics.common.config.CommonProperties;
import lombok.RequiredArgsConstructor;
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
 * Explicit Brave / Micrometer tracing configuration.
 * <p>Creates a {@link Tracing} bean that Spring Boot's autoconfiguration
 * will bridge into Micrometer's {@code Tracer}.  Propagation uses B3
 * (single + multi-header) so that trace-ids flow across service boundaries.
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(Tracing.class)
@EnableConfigurationProperties(CommonProperties.class)
public class TracingConfig {

    private final CommonProperties commonProperties;

    @Bean
    @ConditionalOnMissingBean(Tracing.class)
    public Tracing tracing(
            @Value("${spring.application.name:unknown-service}") String serviceName) {

        var tracingProps = commonProperties.tracing();
        var sender = URLConnectionSender.create(tracingProps.zipkinEndpoint());
        var spanHandler = AsyncZipkinSpanHandler.create((BytesMessageSender) sender);

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
