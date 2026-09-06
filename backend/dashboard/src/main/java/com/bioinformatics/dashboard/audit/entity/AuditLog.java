package com.bioinformatics.dashboard.audit.entity;

import com.bioinformatics.dashboard.audit.dto.AuditAction;
import com.bioinformatics.dashboard.audit.dto.AuditStatus;
import com.bioinformatics.dashboard.audit.dto.AuditTarget;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

import static com.bioinformatics.shared.models.db.DbSchema.GENES_SCHEMA;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_log_seq")
    @SequenceGenerator(name = "audit_log_seq",schema = GENES_SCHEMA, sequenceName = "audit_log_seq", allocationSize = 500)
    private Long id;

    @Column(name = "actor_username", nullable = false)
    private String actorUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 100)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 100)
    private AuditTarget target;

    @Column(name = "target_id")
    private String targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AuditStatus status;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "endpoint", length = 500)
    private String endpoint;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        if (action != null) {
            this.target = action.getDefaultTarget();
        }
    }
}
