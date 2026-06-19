package com.bioinformatics.dashboard.audit.dto;

import java.time.Instant;

public record AuditLogDto(
        long id,
        String username,
        AuditAction action,
        AuditTarget target,
        String targetId,
        AuditStatus status,
        String ipAddress,
        String httpMethod,
        String endpoint,
        Instant createdAt
) {
}
