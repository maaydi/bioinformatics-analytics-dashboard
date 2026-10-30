package com.bioinformatics.auditservice.support;

import com.bioinformatics.shared.models.audit.AuditAction;
import com.bioinformatics.shared.models.audit.AuditStatus;
import com.bioinformatics.shared.models.audit.AuditTarget;
import com.bioinformatics.shared.models.kafka.events.AuditLogEvent;

import java.time.Instant;
import java.util.UUID;

public final class AuditTestFixtures {

    public static final Instant CREATED_AT = Instant.parse("2026-01-15T12:00:00Z");

    private AuditTestFixtures() {
    }

    public static AuditLogEvent event(Object... metadata) {
        return new AuditLogEvent(UUID.randomUUID(), "alice", "USER", "auth-service",
                "request-123", AuditAction.LOGIN, AuditTarget.AUTH, "alice",
                AuditStatus.SUCCESS, "192.0.2.10", CREATED_AT, metadata);
    }
}
