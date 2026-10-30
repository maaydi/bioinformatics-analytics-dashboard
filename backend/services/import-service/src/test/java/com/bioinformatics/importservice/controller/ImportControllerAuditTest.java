package com.bioinformatics.importservice.controller;

import com.bioinformatics.common.audit.annotation.Auditable;
import com.bioinformatics.common.audit.aspect.AuditAspect;
import com.bioinformatics.common.audit.service.AuditEventPublisher;
import com.bioinformatics.importservice.dto.ImportJobSummary;
import com.bioinformatics.importservice.dto.ImportStatus;
import com.bioinformatics.importservice.service.ImportService;
import com.bioinformatics.shared.models.audit.AuditAction;
import com.bioinformatics.shared.models.audit.AuditStatus;
import com.bioinformatics.shared.models.audit.AuditTarget;
import com.bioinformatics.shared.models.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportControllerAuditTest {
    @Mock
    private ImportService service;
    @Mock
    private AuditEventPublisher publisher;
    private ImportController controller;
    private final ImportJobSummary summary =
            new ImportJobSummary("job-123", ImportStatus.RUNNING, "uniprot.dat", 0, 0, 0, null, null, null);

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        var factory = new AspectJProxyFactory(new ImportController(service));
        factory.addAspect(new AuditAspect(publisher));
        controller = factory.getProxy();
    }

    @Test
    void auditsAcceptedFileImportByGeneratedJobId() {
        var file = new MockMultipartFile("file", "uniprot.dat", "text/plain", new byte[0]);
        when(service.triggerImport(file, "APPEND")).thenReturn(summary);

        assertThat(controller.triggerImport(file, "APPEND").getBody()).isSameAs(summary);

        verify(publisher).publish(AuditAction.IMPORT_UPLOAD, AuditTarget.IMPORT_JOB,
                "job-123", AuditStatus.SUCCESS, null);
    }

    @Test
    void auditsAcceptedRemoteImportByGeneratedJobId() {
        var admin = new UserPrincipal("admin", List.of("ROLE_ADMIN"), "postgres");
        when(service.triggerRemoteImport(42L, admin)).thenReturn(summary);

        controller.triggerRemoteImport(42L, admin);

        verify(publisher).publish(AuditAction.IMPORT_REMOTE_TRIGGER, AuditTarget.IMPORT_JOB,
                "job-123", AuditStatus.SUCCESS, null);
    }

    @Test
    void failureBeforeJobCreationHasNoInventedJobId() {
        var file = new MockMultipartFile("file", new byte[0]);
        var original = new IllegalStateException("already running");
        when(service.triggerImport(file, "APPEND")).thenThrow(original);

        assertThatThrownBy(() -> controller.triggerImport(file, "APPEND")).isSameAs(original);

        verify(publisher).publish(AuditAction.IMPORT_UPLOAD, AuditTarget.IMPORT_JOB,
                "N/A", AuditStatus.FAILURE, null);
    }

    @Test
    void routineListingsAndProgressPollingAreNotAudited() throws Exception {
        assertThat(ImportController.class.getMethod("listImportJobs", int.class, int.class)
                .getAnnotation(Auditable.class)).isNull();
        assertThat(ImportController.class.getMethod("getImportJobStatus", String.class)
                .getAnnotation(Auditable.class)).isNull();
    }
}
