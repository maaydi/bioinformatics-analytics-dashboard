package com.bioinformatics.auditservice.controller;

import com.bioinformatics.auditservice.dto.AuditLogDto;
import com.bioinformatics.auditservice.dto.AuditSearchRequest;
import com.bioinformatics.auditservice.service.AuditService;
import com.bioinformatics.common.audit.annotation.Auditable;
import com.bioinformatics.common.models.PagedResponse;
import com.bioinformatics.shared.models.audit.AuditAction;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditService auditService;

    @PostMapping
    @Auditable(action = AuditAction.AUDIT_SEARCH)
    public PagedResponse<AuditLogDto> listAuditLogs(@RequestBody @Valid AuditSearchRequest request){

        return auditService.listAuditLogs(request);

    }
}
