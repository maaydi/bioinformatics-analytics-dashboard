package com.bioinformatics.dashboard.audit.repository;

import com.bioinformatics.dashboard.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByActor_Id(Long actorId, Pageable pageable);
}
