package com.bioinformatics.exportservice.controller;

import com.bioinformatics.common.audit.annotation.Auditable;
import com.bioinformatics.common.audit.aspect.AuditAspect;
import com.bioinformatics.common.audit.service.AuditEventPublisher;
import com.bioinformatics.exportservice.dto.DefaultExportFormat;
import com.bioinformatics.exportservice.dto.ExportFileStream;
import com.bioinformatics.exportservice.dto.ExportPipelineCreateRequest;
import com.bioinformatics.exportservice.dto.ExportPipelineResponse;
import com.bioinformatics.exportservice.service.ExportPipelineService;
import com.bioinformatics.exportservice.writer.ExportFormatWriter;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportPipelineControllerAuditTest {
    @Mock
    private ExportPipelineService service;
    @Mock
    private AuditEventPublisher publisher;
    private ExportPipelineController controller;
    private final UserPrincipal user = new UserPrincipal("alice", List.of("ROLE_USER"), "postgres");

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        var factory = new AspectJProxyFactory(new ExportPipelineController(service));
        factory.addAspect(new AuditAspect(publisher));
        controller = factory.getProxy();
    }

    @Test
    void auditsCreationByGeneratedPipelineId() {
        var request = mock(ExportPipelineCreateRequest.class);
        var response = mock(ExportPipelineResponse.class);
        when(response.id()).thenReturn(123L);
        when(service.createPipeline(request, user)).thenReturn(response);

        controller.createPipeline(request, user);

        verify(publisher).publish(AuditAction.EXPORT_CREATE, AuditTarget.EXPORT_PIPELINE,
                "123", AuditStatus.SUCCESS, null);
    }

    @Test
    void failedCreationDoesNotInventAnId() {
        var request = mock(ExportPipelineCreateRequest.class);
        var original = new IllegalArgumentException("Invalid fields");
        when(service.createPipeline(request, user)).thenThrow(original);

        assertThatThrownBy(() -> controller.createPipeline(request, user)).isSameAs(original);

        verify(publisher).publish(AuditAction.EXPORT_CREATE, AuditTarget.EXPORT_PIPELINE,
                "N/A", AuditStatus.FAILURE, null);
    }

    @Test
    void auditsRetryAndDeletionUsingTheAffectedPipeline() {
        controller.retry(123L, user);
        controller.deletePipeline(456L, user);

        verify(publisher).publish(AuditAction.EXPORT_RETRY, AuditTarget.EXPORT_PIPELINE,
                "123", AuditStatus.SUCCESS, null);
        verify(publisher).publish(AuditAction.EXPORT_DELETE, AuditTarget.EXPORT_PIPELINE,
                "456", AuditStatus.SUCCESS, null);
    }

    @Test
    void auditsDownloadUrlIssuanceWithoutRecordingTheUrl() throws Exception {
        controller.getPipelineDownloadUrl(123L, user);

        verify(publisher).publish(AuditAction.EXPORT_DOWNLOAD_REQUEST, AuditTarget.EXPORT_PIPELINE,
                "123", AuditStatus.SUCCESS, null);
    }

    @Test
    void auditsDownloadFileResponseWithoutInspectingTheStream() throws Exception {
        var writer = mock(ExportFormatWriter.class);
        when(writer.getFormat()).thenReturn(DefaultExportFormat.CSV);
        var stream = new ByteArrayInputStream(new byte[]{1, 2, 3});
        when(service.getExportFileStream(123L, user))
                .thenReturn(new ExportFileStream(stream, "export.csv", 3, writer));

        controller.downloadFile(123L, user);

        assertThat(stream.available()).isEqualTo(3);
        verify(publisher).publish(AuditAction.EXPORT_DOWNLOAD, AuditTarget.EXPORT_PIPELINE,
                "123", AuditStatus.SUCCESS, null);
    }

    @Test
    void detailReadsAuditFailuresOnlyToDetectUnauthorizedAccess() {
        controller.getPipeline(123L, user);
        verifyNoInteractions(publisher);
        var original = new AccessDeniedException("Not owner");
        when(service.getPipeline(123L, user)).thenThrow(original);

        assertThatThrownBy(() -> controller.getPipeline(123L, user)).isSameAs(original);

        verify(publisher).publish(AuditAction.DETAIL_VIEW, AuditTarget.EXPORT_PIPELINE,
                "123", AuditStatus.FAILURE, null);
    }

    @Test
    void listsPollingAndFieldMetadataStayUnaudited() {
        var excluded = Set.of("listPipelines", "getPipelineJobStatus", "getAvailableFields");
        assertThat(ExportPipelineController.class.getDeclaredMethods())
                .filteredOn(method -> excluded.contains(method.getName()))
                .hasSize(3)
                .allSatisfy(method -> assertThat(method.getAnnotation(Auditable.class)).isNull());
    }
}
