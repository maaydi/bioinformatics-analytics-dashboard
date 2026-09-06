package com.bioinformatics.dashboard.audit.service;

import com.bioinformatics.dashboard.audit.dto.AuditAction;
import com.bioinformatics.dashboard.audit.dto.AuditLogDto;
import com.bioinformatics.dashboard.audit.dto.AuditStatus;
import com.bioinformatics.dashboard.audit.dto.AuditWebDetails;
import com.bioinformatics.dashboard.audit.entity.AuditLog;
import com.bioinformatics.dashboard.audit.mapper.AuditLogMapper;
import com.bioinformatics.dashboard.audit.repository.AuditLogRepository;
import com.bioinformatics.shared.models.security.UserPrincipal;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Manages operations and logic for AuditService.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuditService {
    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper mapper;

    /**
     * Persist an audit log entry asynchronously.
     *
     * @param actor             the authenticated user performing the action, or null for anonymous/system
     * @param attemptedUsername the username attempted or acting username when actor is null
     * @param action            the audited action
     * @param targetId          identifier of the target resource (maybe null)
     * @param status            the audit status
     * @param webDetails        optional web request details
     */
    @Async("auditExecutor")
    public void save(UserPrincipal actor, String attemptedUsername, AuditAction action, String targetId, AuditStatus status, AuditWebDetails webDetails) {
        var auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setTarget(action.getDefaultTarget());
        auditLog.setTargetId(targetId);
        auditLog.setStatus(status);
        if (actor != null) {
            auditLog.setActorUsername(actor.id());
        } else {
            auditLog.setActorUsername(attemptedUsername != null ? attemptedUsername : "UNKNOWN");
        }
        if (webDetails != null) {
            auditLog.setHttpMethod(webDetails.httpMethod());
            auditLog.setEndpoint(webDetails.endpoint());
            auditLog.setIpAddress(webDetails.ipAddress());
        } else {
            auditLog.setHttpMethod("SYSTEM");
            auditLog.setEndpoint("INTERNAL");
        }
        auditLogRepository.save(auditLog);
    }

    /**
     * Retrieve paginated audit log entries for a given user.
     *
     * @param username   the actor/user id to filter by
     * @param pageable pagination information
     * @return page of AuditLogDto entries
     */
    public Page<AuditLogDto> findByUser(String username, Pageable pageable) {
        return auditLogRepository.findByActorUsername(username, pageable).map(mapper::toDto);
    }
}
