package com.bioinformatics.auditservice.entity;

import com.bioinformatics.shared.models.audit.AuditAction;
import com.bioinformatics.shared.models.audit.AuditTarget;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogTest {

    @Test
    void creationAssignsThePersistenceTimeAndActionDefaultTarget() {
        var entity = AuditLog.builder().createdAt(Instant.EPOCH)
                .action(AuditAction.FILTER_SAVE).targetType(AuditTarget.DEFAULT).build();
        var before = Instant.now();

        entity.onCreate();

        assertThat(entity.getCreatedAt()).isBetween(before, Instant.now());
        assertThat(entity.getTargetType()).isEqualTo(AuditTarget.SAVED_FILTER);
    }

    @Test
    void creationWithoutAnActionLeavesTheTargetUnchanged() {
        var entity = AuditLog.builder().targetType(AuditTarget.DEFAULT).build();
        var before = Instant.now();

        entity.onCreate();

        assertThat(entity.getCreatedAt()).isBetween(before, Instant.now());
        assertThat(entity.getTargetType()).isEqualTo(AuditTarget.DEFAULT);
    }
}
