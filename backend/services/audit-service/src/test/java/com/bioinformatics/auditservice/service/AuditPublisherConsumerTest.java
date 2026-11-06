package com.bioinformatics.auditservice.service;

import com.bioinformatics.common.audit.config.AuditPublisherConfig;
import com.bioinformatics.common.audit.dto.AuditWebDetails;
import com.bioinformatics.common.audit.service.AuditContextHolder;
import com.bioinformatics.common.audit.service.AuditEventPublisher;
import com.bioinformatics.common.config.CommonProperties;
import com.bioinformatics.common.config.kafka.KafkaConsumerConfig;
import com.bioinformatics.common.config.kafka.KafkaProducerConfig;
import com.bioinformatics.common.correlation.CorrelationContext;
import com.bioinformatics.shared.models.audit.AuditAction;
import com.bioinformatics.shared.models.audit.AuditStatus;
import com.bioinformatics.shared.models.audit.AuditTarget;
import com.bioinformatics.shared.models.kafka.events.AuditLogEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.bioinformatics.shared.models.kafka.KafkaTopics.AUDIT_EVENTS_LOG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(AuditPublisherConsumerTest.Config.class)
@EmbeddedKafka(partitions = 1, topics = AUDIT_EVENTS_LOG)
@DirtiesContext
@TestPropertySource(properties = {
        "spring.application.name=publisher-contract-test",
        "app.audit.enabled=true",
        "common.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "common.kafka.consumer.group-id=audit-logger",
        "common.kafka.consumer.concurrency=1",
        "common.kafka.consumer.ack-mode=manual"
})
class AuditPublisherConsumerTest {

    @Autowired
    private AuditEventPublisher publisher;
    @Autowired
    private AuditService service;
    @Autowired
    private AuditIdempotencyService idempotencyService;
    @Autowired
    private RecordObserver observer;

    @Test
    void sharedPublisherDeliversCompatibleEventToAuditConsumer() throws Exception {
        doAnswer(invocation -> {
            assertThat(MDC.get(CorrelationContext.MDC_KEY)).isEqualTo("request-123");
            return null;
        }).when(service).save(any(AuditLogEvent.class));
        AuditContextHolder.set(new AuditWebDetails("POST", "/filters", "127.0.0.1", "request-123"));
        try {
            publisher.publish(AuditAction.FILTER_SAVE, AuditTarget.SAVED_FILTER, "42", AuditStatus.SUCCESS);
        } finally {
            AuditContextHolder.clear();
        }

        var captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(service, timeout(20000)).save(captor.capture());
        var event = captor.getValue();
        assertThat(event.eventId()).isNotNull();
        assertThat(event.sourceService()).isEqualTo("publisher-contract-test");
        assertThat(event.correlationId()).isEqualTo("request-123");
        assertThat(event.action()).isEqualTo(AuditAction.FILTER_SAVE);
        assertThat(event.targetType()).isEqualTo(AuditTarget.SAVED_FILTER);
        assertThat(event.targetId()).isEqualTo("42");
        assertThat(event.status()).isEqualTo(AuditStatus.SUCCESS);
        assertThat(event.ipAddress()).isEqualTo("127.0.0.1");
        assertThat(event.createdAt()).isNotNull();
        assertThat(event.metadata()).containsExactly("httpMethod", "POST", "endpoint", "/filters");
        verify(idempotencyService, timeout(5000)).markProcessed(event.eventId());
        assertThat(observer.received.await(20, TimeUnit.SECONDS)).isTrue();
        assertThat(observer.record.headers().lastHeader("__TypeId__")).isNull();
        assertThat(observer.record.headers().lastHeader(CorrelationContext.HEADER)).isNotNull();
    }

    static class RecordObserver {
        final CountDownLatch received = new CountDownLatch(1);
        ConsumerRecord<String, AuditLogEvent> record;

        @KafkaListener(topics = AUDIT_EVENTS_LOG, groupId = "audit-header-observer", properties = {
                "spring.json.value.default.type=com.bioinformatics.shared.models.kafka.events.AuditLogEvent",
                "spring.json.use.type.headers=false"
        })
        void observe(ConsumerRecord<String, AuditLogEvent> incoming, Acknowledgment ack) {
            record = incoming;
            ack.acknowledge();
            received.countDown();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableKafka
    @EnableConfigurationProperties(CommonProperties.class)
    @Import({AuditPublisherConfig.class, KafkaProducerConfig.class, KafkaConsumerConfig.class, AuditConsumer.class})
    static class Config {
        @Bean
        RecordObserver recordObserver() {
            return new RecordObserver();
        }

        @Bean
        AuditService auditService() {
            return mock(AuditService.class);
        }

        @Bean
        AuditIdempotencyService auditIdempotencyService() {
            return mock(AuditIdempotencyService.class);
        }
    }
}
