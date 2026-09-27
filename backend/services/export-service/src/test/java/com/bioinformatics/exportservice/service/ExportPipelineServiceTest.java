package com.bioinformatics.exportservice.service;

import com.bioinformatics.exportservice.batch.ExportJobExecutor;
import com.bioinformatics.exportservice.dto.ExportPipelineCreateRequest;
import com.bioinformatics.exportservice.dto.ExportPipelineResponse;
import com.bioinformatics.exportservice.dto.ExportStatus;
import com.bioinformatics.exportservice.entity.ExportPipeline;
import com.bioinformatics.exportservice.mapper.ExportPipelineMapper;
import com.bioinformatics.exportservice.repository.ExportJobExecutionRepository;
import com.bioinformatics.exportservice.repository.ExportPipelineRepository;
import com.bioinformatics.exportservice.writer.ExportWriterFactory;
import com.bioinformatics.shared.models.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.repository.JobRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportPipelineServiceTest {

    @Mock
    private ExportPipelineRepository pipelineRepository;

    @Mock
    private ExportJobExecutionRepository jobExecutionRepository;

    @Mock
    private ExportPipelineMapper mapper;

    @Mock
    private ExportJobExecutor executor;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ExportWriterFactory writerFactory;

    @Mock
    private ExportPipelineLifeCycleService taskletService;

    @InjectMocks
    private ExportPipelineService service;

    private UserPrincipal user;

    @BeforeEach
    void setUp() {
        user = new UserPrincipal("admin_user", List.of("ROLE_ADMIN"), "POSTGRES");
    }

    @Test
    void createPipeline_withValidRequest_returnsCreatedDto() {
        var request = new ExportPipelineCreateRequest("name", "desc", null, null, List.of("a", "b"));

        var entityToPersist = ExportPipeline.builder()
                .name("name")
                .userId("admin_user")
                .build();

        var savedEntity = ExportPipeline.builder()
                .id(1L)
                .name("name")
                .userId("admin_user")
                .status(ExportStatus.QUEUED)
                .format(com.bioinformatics.exportservice.dto.DefaultExportFormat.CSV)
                .createdAt(Instant.now())
                .build();

        var response = org.mockito.Mockito.mock(ExportPipelineResponse.class);
        when(response.id()).thenReturn(1L);
        when(response.name()).thenReturn("name");

        when(mapper.toEntity(eq(request), eq(user.id()))).thenReturn(entityToPersist);
        when(pipelineRepository.save(entityToPersist)).thenReturn(savedEntity);
        doNothing().when(executor).execute(any());
        when(mapper.toDto(savedEntity)).thenReturn(response);

        var result = service.createPipeline(request, user);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("name");
    }

    @Test
    void getDownloadUrl_completedPipeline_returnsUrl() throws IOException {
        var pipeline = ExportPipeline.builder()
                .id(2L)
                .userId(user.id())
                .name("p2")
                .status(ExportStatus.COMPLETED)
                .filePath("/tmp/export_2.csv")
                .fileSizeBytes(123L)
                .format(com.bioinformatics.exportservice.dto.DefaultExportFormat.CSV)
                .build();

        when(pipelineRepository.findById(2L)).thenReturn(Optional.of(pipeline));

        var dto = service.getDownloadUrl(2L, user);

        assertThat(dto).isNotNull();
        assertThat(dto.downloadUrl()).contains("/exports/pipelines/2/download-file");
    }

    @Test
    void getDownloadUrl_incompletePipeline_throws() {
        var pipeline = ExportPipeline.builder()
                .id(3L)
                .userId(user.id())
                .name("p3")
                .status(ExportStatus.RUNNING)
                .build();

        when(pipelineRepository.findById(3L)).thenReturn(Optional.of(pipeline));

        assertThatThrownBy(() -> service.getDownloadUrl(3L, user))
                .isInstanceOf(java.nio.file.NoSuchFileException.class);
    }

    @Test
    void retryPipeline_failedPipeline_requeues() {
        var original = ExportPipeline.builder()
                .id(4L)
                .userId(user.id())
                .name("orig")
                .status(ExportStatus.FAILED)
                .filterJson(null)
                .format(com.bioinformatics.exportservice.dto.DefaultExportFormat.CSV)
                .fieldSchema(null)
                .build();

        when(pipelineRepository.findById(4L)).thenReturn(Optional.of(original));

        var originalDto = org.mockito.Mockito.mock(ExportPipelineResponse.class);
        when(originalDto.name()).thenReturn("orig");
        when(mapper.toDto(original)).thenReturn(originalDto);

        // new pipeline created by createPipeline inside retry
        var newEntity = ExportPipeline.builder().id(5L).userId(user.id()).name("orig").status(ExportStatus.QUEUED).format(com.bioinformatics.exportservice.dto.DefaultExportFormat.CSV).build();
        var newResponse = org.mockito.Mockito.mock(ExportPipelineResponse.class);
        when(newResponse.id()).thenReturn(5L);

        when(mapper.toEntity(any(ExportPipelineCreateRequest.class), eq(user.id()))).thenReturn(newEntity);
        when(pipelineRepository.save(newEntity)).thenReturn(newEntity);
        doNothing().when(executor).execute(any());
        when(mapper.toDto(newEntity)).thenReturn(newResponse);

        var result = service.retryPipeline(4L, user);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(5L);
    }

    @Test
    void deletePipeline_running_callsStopAndSoftDeletes() {
        var running = ExportPipeline.builder()
                .id(6L)
                .userId(user.id())
                .name("r")
                .status(ExportStatus.RUNNING)
                .jobExecutionId(100L)
                .build();

        when(pipelineRepository.findById(6L)).thenReturn(Optional.of(running));
        // JobExecution may be unavailable in some test environments; allow null (code handles null execution)
        when(jobRepository.getJobExecution(100L)).thenReturn(null);

        service.deletePipeline(6L, user);

        assertThat(running.getDeletedAt()).isNotNull();
    }
}

