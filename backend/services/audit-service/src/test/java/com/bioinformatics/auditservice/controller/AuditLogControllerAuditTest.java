package com.bioinformatics.auditservice.controller;

import com.bioinformatics.auditservice.dto.AuditLogDto;
import com.bioinformatics.auditservice.dto.AuditSearchRequest;
import com.bioinformatics.auditservice.service.AuditService;
import com.bioinformatics.common.audit.aspect.AuditAspect;
import com.bioinformatics.common.audit.service.AuditEventPublisher;
import com.bioinformatics.common.models.PagedResponse;
import com.bioinformatics.shared.models.audit.AuditAction;
import com.bioinformatics.shared.models.audit.AuditStatus;
import com.bioinformatics.shared.models.audit.AuditTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogControllerAuditTest {
    @Mock
    private AuditService service;
    @Mock
    private AuditEventPublisher publisher;
    private AuditLogController controller;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        var factory = new AspectJProxyFactory(new AuditLogController(service));
        factory.addAspect(new AuditAspect(publisher));
        controller = factory.getProxy();
    }

    @Test
    void auditsAccessToAuditHistoryWithoutRepublishingItsContents() {
        var request = AuditSearchRequest.builder().page(0).size(20).build();
        var page = new PagedResponse<AuditLogDto>(List.of(), 0, 20, 0L, 0);
        when(service.listAuditLogs(request)).thenReturn(page);

        assertThat(controller.listAuditLogs(request)).isSameAs(page);

        verify(publisher).publish(AuditAction.AUDIT_SEARCH, AuditTarget.AUDIT, "N/A", AuditStatus.SUCCESS, null);
    }

    @Test
    void failedAuditHistoryAccessIsAudited() {
        var request = AuditSearchRequest.builder().page(0).size(20).build();
        var original = new IllegalStateException("Storage unavailable");
        when(service.listAuditLogs(request)).thenThrow(original);

        assertThatThrownBy(() -> controller.listAuditLogs(request)).isSameAs(original);

        verify(publisher).publish(AuditAction.AUDIT_SEARCH, AuditTarget.AUDIT, "N/A", AuditStatus.FAILURE, null);
    }
}
