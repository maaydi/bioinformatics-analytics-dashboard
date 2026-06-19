package com.bioinformatics.dashboard.audit.service;

import com.bioinformatics.dashboard.audit.dto.AuditAction;
import com.bioinformatics.dashboard.audit.dto.AuditStatus;
import com.bioinformatics.dashboard.audit.entity.AuditLog;
import com.bioinformatics.dashboard.audit.repository.AuditLogRepository;
import com.bioinformatics.dashboard.auth.entity.AppUser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    @Async
    public void save(AppUser actor, AuditAction action, String targetId, AuditStatus status,
                     String httpMethod, String endpoint, String ipAddress) {
        var auditLog = new AuditLog();
        auditLog.setActor(actor);
        auditLog.setAction(action);
        auditLog.setTarget(action.getDefaultTarget());
        auditLog.setTargetId(targetId);
        auditLog.setStatus(status);
        auditLog.setHttpMethod(httpMethod);
        auditLog.setEndpoint(endpoint);
        auditLog.setIpAddress(ipAddress);
        auditLogRepository.save(auditLog);
    }

    public Page<AuditLog> findByUserId(Long userId, Pageable pageable) {
        return auditLogRepository.findByActor_Id(userId, pageable);
    }
}
