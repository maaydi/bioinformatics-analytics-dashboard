package com.bioinformatics.analyticsservice.materializeviews.service;

import com.bioinformatics.common.config.kafka.KafkaConsumerConfig;
import com.bioinformatics.common.config.kafka.KafkaProducerConfig;
import com.bioinformatics.common.correlation.CorrelationContext;
import com.bioinformatics.shared.models.kafka.events.ViewRefreshRequestedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.bioinformatics.shared.models.kafka.KafkaTopics.PROTEIN_EVENTS_IMPORTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(ViewRefreshTransportTest.Config.class)
@EmbeddedKafka(partitions = 1, topics = PROTEIN_EVENTS_IMPORTED)
@DirtiesContext
@TestPropertySource(properties = {
        "common.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "common.kafka.consumer.group-id=legacy-refresh-contract",
        "common.kafka.consumer.concurrency=1",
        "common.kafka.consumer.ack-mode=manual"
})
class ViewRefreshTransportTest {
    @Autowired
    KafkaTemplate<String, Object> kafka;
    @Autowired
    MaterializedViewRefreshService service;
    @Autowired
    RefreshIdempotencyService idempotency;
    @Autowired
    RecordObserver observer;

    @Test
    void realLegacyConsumerAcceptsHeaderFreeEventsAndReceivesCorrelation() throws Exception {
        doAnswer(invocation -> {
            assertThat(MDC.get(CorrelationContext.MDC_KEY)).isEqualTo("refresh-request-123");
            return null;
        }).when(service).refreshAllDashboardViews("job-123");
        var event = new ViewRefreshRequestedEvent("job-123", "test", Instant.now(), "refresh-request-123");
        try (var ignored = CorrelationContext.open(event.correlationId())) {
            kafka.send(PROTEIN_EVENTS_IMPORTED, event.jobId(), event).get(10, TimeUnit.SECONDS);
        }
        verify(service, timeout(20000)).refreshAllDashboardViews("job-123");
        verify(idempotency, timeout(5000)).markProcessed("job-123");
        assertThat(observer.received.await(20, TimeUnit.SECONDS)).isTrue();
        assertThat(observer.record.value()).isEqualTo(event);
        assertThat(observer.record.headers().lastHeader("__TypeId__")).isNull();
        assertThat(observer.record.headers().lastHeader(CorrelationContext.HEADER)).isNotNull();
    }

    static class RecordObserver {
        final CountDownLatch received = new CountDownLatch(1);
        ConsumerRecord<String, ViewRefreshRequestedEvent> record;

        @KafkaListener(topics = PROTEIN_EVENTS_IMPORTED, groupId = "legacy-refresh-header-observer",
                properties = {
                        "spring.json.value.default.type=com.bioinformatics.shared.models.kafka.events.ViewRefreshRequestedEvent",
                        "spring.json.use.type.headers=false"
                })
        void observe(ConsumerRecord<String, ViewRefreshRequestedEvent> incoming, Acknowledgment ack) {
            record = incoming;
            ack.acknowledge();
            received.countDown();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableKafka
    @Import({KafkaProducerConfig.class, KafkaConsumerConfig.class, ViewRefreshConsumer.class})
    static class Config {
        @Bean
        MaterializedViewRefreshService refreshService() {
            return mock(MaterializedViewRefreshService.class);
        }

        @Bean
        RefreshIdempotencyService refreshIdempotencyService() {
            return mock(RefreshIdempotencyService.class);
        }

        @Bean
        RecordObserver recordObserver() {
            return new RecordObserver();
        }
    }
}
