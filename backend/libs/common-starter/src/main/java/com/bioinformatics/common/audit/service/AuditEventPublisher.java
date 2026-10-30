package com.bioinformatics.common.audit.service;

import com.bioinformatics.shared.models.audit.AuditAction;
import com.bioinformatics.shared.models.audit.AuditStatus;
import com.bioinformatics.shared.models.audit.AuditTarget;
import com.bioinformatics.shared.models.kafka.events.AuditLogEvent;
import com.bioinformatics.shared.models.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

import static com.bioinformatics.shared.models.kafka.KafkaTopics.AUDIT_EVENTS_LOG;

@Slf4j
public class AuditEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TaskExecutor executor;
    private final String sourceService;

    public AuditEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, TaskExecutor executor, String sourceService) {
        Assert.hasText(sourceService, "spring.application.name is required for audit publishing");
        this.kafkaTemplate = kafkaTemplate;
        this.executor = executor;
        this.sourceService = sourceService;
    }

    public void publish(AuditAction action, AuditTarget target, String targetId, AuditStatus status) {
        publish(action, target, targetId, status, SecurityContextHolder.getContext().getAuthentication());
    }

    public void publish(AuditAction action, AuditTarget target, String targetId, AuditStatus status,
                        Authentication authentication) {
        Assert.notNull(action, "Audit action is required");
        Assert.notNull(target, "Audit target is required");
        Assert.notNull(status, "Audit status is required");
        var details = AuditContextHolder.get();
        var authenticated = authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        var actorIdentity = authenticated
                ? authentication.getPrincipal() instanceof UserPrincipal user ? user.id() : authentication.getName()
                : "UNKNOWN";
        var actorType = authenticated ? "USER" : details == null ? "SYSTEM" : "ANONYMOUS";
        var correlationId = details != null ? details.correlationId() : null;
        if (!StringUtils.hasText(correlationId)) {
            correlationId = MDC.get("traceId");
        }
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }
        // Snapshot thread-bound context before handing work to the executor.
        var event = new AuditLogEvent(UUID.randomUUID(), actorIdentity, actorType, sourceService,
                correlationId, action, target == AuditTarget.DEFAULT ? action.getDefaultTarget() : target,
                targetId, status, details != null ? details.ipAddress() : null, Instant.now(),
                "httpMethod", details != null ? details.httpMethod() : "SYSTEM",
                "endpoint", details != null ? details.endpoint() : "INTERNAL");
        try {
            executor.execute(() -> send(event));
        } catch (TaskRejectedException error) {
            log.error("Audit publish queue rejected eventId={} sourceService={}", event.eventId(), sourceService, error);
        }
    }

    private void send(AuditLogEvent event) {
        try {
            kafkaTemplate.send(AUDIT_EVENTS_LOG, event.eventId().toString(), event)
                    .whenComplete((ignored, error) -> {
                        if (error != null) {
                            log.error("Audit publish failed eventId={} sourceService={}",
                                    event.eventId(), sourceService, error);
                        }
                    });
        } catch (RuntimeException error) {
            log.error("Audit send failed eventId={} sourceService={}", event.eventId(), sourceService, error);
        }
    }
}
