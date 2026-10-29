package com.bioinformatics.auditservice.repository;

import com.bioinformatics.auditservice.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    /**
     * Find audit log entries for a specific actor (user) with pagination.
     *
     * @param actorUsername the username of the actor/user
     * @param pageable      pagination information
     * @return a page of matching AuditLog entities
     */
    Page<AuditLog> findByActorIdentity(String actorUsername, Pageable pageable);
}
