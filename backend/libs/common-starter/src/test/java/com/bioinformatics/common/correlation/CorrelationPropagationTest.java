package com.bioinformatics.common.correlation;

import com.bioinformatics.common.config.correlation.FeignCorrelationAutoConfiguration;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorrelationPropagationTest {
    @AfterEach
    void clearContext() {
        MDC.clear();
    }

    @Test
    void servletFeignKafkaChainKeepsOneCorrelationAndRestoresWorkerContext() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(FeignCorrelationAutoConfiguration.class))
                .run(context -> {
                    var request = new MockHttpServletRequest();
                    request.addHeader(CorrelationContext.HEADER, "request-123");
                    var response = new MockHttpServletResponse();
                    MDC.put(CorrelationContext.MDC_KEY, "previous");
                    new CorrelationFilter().doFilter(request, response, (incoming, outgoing) -> {
                        assertThat(MDC.get(CorrelationContext.MDC_KEY)).isEqualTo("request-123");
                        var template = new RequestTemplate();
                        context.getBean(RequestInterceptor.class).apply(template);
                        assertThat(template.headers().get(CorrelationContext.HEADER)).containsExactly("request-123");

                        var producerRecord = new ProducerRecord<String, Object>("topic", "key", "value");
                        new CorrelationProducerInterceptor().onSend(producerRecord);
                        var consumerRecord = new ConsumerRecord<String, Object>("topic", 0, 0, "key", "value");
                        producerRecord.headers().forEach(consumerRecord.headers()::add);
                        var interceptor = new CorrelationRecordInterceptor();
                        MDC.put(CorrelationContext.MDC_KEY, "consumer-worker");
                        interceptor.intercept(consumerRecord, null);
                        assertThat(MDC.get(CorrelationContext.MDC_KEY)).isEqualTo("request-123");
                        interceptor.afterRecord(consumerRecord, null);
                        assertThat(MDC.get(CorrelationContext.MDC_KEY)).isEqualTo("consumer-worker");
                    });
                    assertThat(response.getHeader(CorrelationContext.HEADER)).isEqualTo("request-123");
                    assertThat(MDC.get(CorrelationContext.MDC_KEY)).isEqualTo("previous");
                });
    }

    @Test
    void servletFailureAndMissingOrInvalidHeadersDoNotLeakContext() {
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationContext.HEADER, "invalid\nheader");
        var response = new MockHttpServletResponse();
        assertThatThrownBy(() -> new CorrelationFilter().doFilter(request, response, (incoming, outgoing) -> {
            assertThat(MDC.get(CorrelationContext.MDC_KEY)).isNotBlank().doesNotContain("\n");
            throw new IllegalStateException("business failure");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(MDC.get(CorrelationContext.MDC_KEY)).isNull();
        assertThat(CorrelationContext.normalize(null)).isNotBlank();
        assertThat(CorrelationContext.normalize("x".repeat(101))).hasSize(36);
    }

    @Test
    void consumerFailureRestoresPriorContextAndNeverRetainsPreviousRecord() {
        var interceptor = new CorrelationRecordInterceptor();
        var record = new ConsumerRecord<String, Object>("topic", 0, 0, "key", "value");
        record.headers().add(CorrelationContext.HEADER, "event-123".getBytes(StandardCharsets.UTF_8));
        interceptor.intercept(record, null);
        assertThat(MDC.get(CorrelationContext.MDC_KEY)).isEqualTo("event-123");
        interceptor.failure(record, new IllegalStateException("failed"), null);
        interceptor.afterRecord(record, null);
        assertThat(MDC.get(CorrelationContext.MDC_KEY)).isNull();

        var next = new ConsumerRecord<String, Object>("topic", 0, 1, "key", "value");
        interceptor.intercept(next, null);
        assertThat(MDC.get(CorrelationContext.MDC_KEY)).isNotBlank().isNotEqualTo("event-123");
        interceptor.afterRecord(next, null);
        assertThat(MDC.get(CorrelationContext.MDC_KEY)).isNull();
    }
}
