package com.bioinformatics.dashboard.audit.dto;

import java.time.Instant;

/**
 * Immutable projection of an audit log entry for API responses.
 *
 * @param id         audit record id
 * @param userId     actor user id
 * @param username   actor username
 * @param action     audited action
 * @param target     audited target type
 * @param targetId   identifier of the target resource
 * @param status     audit status (SUCCESS/FAILURE)
 * @param ipAddress  client IP address
 * @param httpMethod HTTP method of the request
 * @param endpoint   request endpoint
 * @param createdAt  timestamp when the audit was created
 */
public record AuditLogDto(
        long id,
        long userId,
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
