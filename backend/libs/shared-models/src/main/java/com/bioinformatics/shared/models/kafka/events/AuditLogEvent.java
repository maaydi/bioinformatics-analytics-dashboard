package com.bioinformatics.shared.models.kafka.events;

import com.bioinformatics.shared.models.audit.AuditAction;
import com.bioinformatics.shared.models.audit.AuditStatus;
import com.bioinformatics.shared.models.audit.AuditTarget;

import java.time.Instant;
import java.util.UUID;

public record AuditLogEvent(UUID eventId, String actorIdentity, String actorType, String sourceService,
                            String correlationId, AuditAction action, AuditTarget targetType, String targetId,
                            AuditStatus status, String ipAddress,
                            Instant createdAt, Object... metadata) {

}
