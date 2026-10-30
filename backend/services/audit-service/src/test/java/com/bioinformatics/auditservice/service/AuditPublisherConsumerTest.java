package com.bioinformatics.auditservice.service;

import com.bioinformatics.common.audit.config.AuditPublisherConfig;
import com.bioinformatics.common.audit.dto.AuditWebDetails;
import com.bioinformatics.common.audit.service.AuditContextHolder;
import com.bioinformatics.common.audit.service.AuditEventPublisher;
import com.bioinformatics.common.config.CommonProperties;
import com.bioinformatics.common.config.kafka.KafkaConsumerConfig;
import com.bioinformatics.common.config.kafka.KafkaProducerConfig;
import com.bioinformatics.shared.models.audit.AuditAction;
import com.bioinformatics.shared.models.audit.AuditStatus;
import com.bioinformatics.shared.models.audit.AuditTarget;
import com.bioinformatics.shared.models.kafka.events.AuditLogEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static com.bioinformatics.shared.models.kafka.KafkaTopics.AUDIT_EVENTS_LOG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(AuditPublisherConsumerTest.Config.class)
@EmbeddedKafka(partitions = 1, topics = AUDIT_EVENTS_LOG)
@DirtiesContext
@TestPropertySource(properties = {
        "spring.application.name=publisher-contract-test",
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

    @Test
    void sharedPublisherDeliversCompatibleEventToAuditConsumer() {
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
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableKafka
    @EnableConfigurationProperties(CommonProperties.class)
    @Import({AuditPublisherConfig.class, KafkaProducerConfig.class, KafkaConsumerConfig.class, AuditConsumer.class})
    static class Config {
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
