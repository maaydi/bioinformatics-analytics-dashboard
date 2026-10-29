package com.bioinformatics.auditservice.dto;

import com.bioinformatics.shared.models.audit.AuditAction;
import com.bioinformatics.shared.models.audit.AuditStatus;
import com.bioinformatics.shared.models.audit.AuditTarget;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable projection of an audit log entry for API responses.
 *
 * @param id            audit record id
 * @param actorIdentity actor username
 * @param action        audited action
 * @param targetType    audited target type
 * @param targetId      identifier of the target resource
 * @param status        audit status (SUCCESS/FAILURE)
 * @param ipAddress     client IP address
 * @param metadata      Other metadata
 * @param createdAt     timestamp when the audit was created
 */
public record AuditLogDto(
        long id,
        UUID eventId,
        String actorIdentity,
        String actorType,
        String sourceService,
        String correlationId,
        AuditAction action,
        AuditTarget targetType,
        String targetId,
        AuditStatus status,
        String ipAddress,
        Map<String, Object> metadata,
        Instant createdAt
) {
}
