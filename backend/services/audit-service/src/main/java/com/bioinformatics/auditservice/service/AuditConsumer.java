package com.bioinformatics.auditservice.service;

import com.bioinformatics.shared.models.kafka.events.AuditLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import static com.bioinformatics.shared.models.kafka.KafkaGroupId.AUDIT_LOGGER;
import static com.bioinformatics.shared.models.kafka.KafkaTopics.AUDIT_EVENTS_LOG;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditConsumer {

    private final AuditIdempotencyService idempotencyService;
    private final AuditService service;

    @KafkaListener(
            topics = AUDIT_EVENTS_LOG,
            groupId = AUDIT_LOGGER,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(@Payload AuditLogEvent event, Acknowledgment ack) {
        if (idempotencyService.isProcessed(event.eventId())) {
            log.warn("Duplicate event for Audit Log {}, skipping", event.eventId());
            ack.acknowledge();
            return;
        }
        log.info("Received Audit log request from {} Action {} (correlationId: {})", event.sourceService(),
                event.action(), event.correlationId());

        try {
            service.save(event);
            idempotencyService.markProcessed(event.eventId());
            ack.acknowledge();
            log.info("Successfully saved audit log {}", event.eventId());
        } catch (Exception e) {
            log.error("Critical failure saving audit log {}", event.eventId(), e);
            throw e;
        }
    }
}
