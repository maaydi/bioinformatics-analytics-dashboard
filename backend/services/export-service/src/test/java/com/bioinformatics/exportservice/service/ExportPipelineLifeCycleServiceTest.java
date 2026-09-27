package com.bioinformatics.exportservice.service;

import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.exportservice.dto.ExportStatus;
import com.bioinformatics.exportservice.entity.ExportJobExecution;
import com.bioinformatics.exportservice.entity.ExportPipeline;
import com.bioinformatics.exportservice.repository.ExportJobExecutionRepository;
import com.bioinformatics.exportservice.repository.ExportPipelineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportPipelineLifeCycleServiceTest {

    @Mock
    private ExportPipelineRepository pipelineRepository;

    @Mock
    private ExportJobExecutionRepository jobExecutionRepository;

    @InjectMocks
    private ExportPipelineLifeCycleService service;

    private ExportPipeline pipeline;

    @BeforeEach
    void setUp() {
        pipeline = new ExportPipeline();
        pipeline.setId(42L);
        pipeline.setStatus(ExportStatus.QUEUED);
        pipeline.setUserId("alice");
        pipeline.setName("demo-export");
        pipeline.setFilterJson(GeneSearchRequest.builder().build());
        pipeline.setStartedAt(Instant.parse("2025-01-01T00:00:00Z"));
    }

    @Test
    void markAsRunning_setsStatusAndCreatesExecutionRecord() {
        when(pipelineRepository.findById(42L)).thenReturn(Optional.of(pipeline));
        when(jobExecutionRepository.save(any(ExportJobExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.markAsRunning(42L, 99L);

        assertThat(pipeline.getStatus()).isEqualTo(ExportStatus.RUNNING);
        assertThat(pipeline.getJobExecutionId()).isEqualTo(99L);
        assertThat(pipeline.getStartedAt()).isNotNull();

        verify(pipelineRepository).save(pipeline);
        verify(jobExecutionRepository).save(argThat(exec ->
                exec.getPipeline().getId().equals(42L)
                        && exec.getJobExecutionId().equals(99L)
        ));
    }

    @Test
    void updatePipelineEstimatedRows_updatesExecutionChunksTotal() {
        var execution = new ExportJobExecution();
        execution.setPipeline(pipeline);
        execution.setChunksTotal(null);

        when(pipelineRepository.findById(42L)).thenReturn(Optional.of(pipeline));
        when(jobExecutionRepository.findByPipelineId(42L)).thenReturn(Optional.of(execution));

        service.updatePipelineEstimatedRows(42L, 250L, 50);

        assertThat(pipeline.getEstimatedRows()).isEqualTo(250L);
        assertThat(execution.getChunksTotal()).isEqualTo(5);
        verify(jobExecutionRepository).save(execution);
    }

    @Test
    void markAsCompleted_setsTerminalStateAndDuration() {
        when(pipelineRepository.findById(42L)).thenReturn(Optional.of(pipeline));

        service.markAsCompleted(42L, "/tmp/export.csv", 123L, 44L);

        assertThat(pipeline.getStatus()).isEqualTo(ExportStatus.COMPLETED);
        assertThat(pipeline.getFilePath()).isEqualTo("/tmp/export.csv");
        assertThat(pipeline.getFileSizeBytes()).isEqualTo(123L);
        assertThat(pipeline.getActualRows()).isEqualTo(44L);
        assertThat(pipeline.getCompletedAt()).isNotNull();
        assertThat(pipeline.getDurationMs()).isNotNull();
        verify(pipelineRepository).save(pipeline);
    }

    @Test
    void markAsFailed_setsFailureStateAndErrorMessage() {
        when(pipelineRepository.findById(42L)).thenReturn(Optional.of(pipeline));

        service.markAsFailed(42L, "bad data");

        assertThat(pipeline.getStatus()).isEqualTo(ExportStatus.FAILED);
        assertThat(pipeline.getErrorMessage()).isEqualTo("bad data");
        assertThat(pipeline.getCompletedAt()).isNotNull();
        verify(pipelineRepository).save(pipeline);
    }

    @Test
    void markAsCancelled_setsCancelledStatus() {
        when(pipelineRepository.findById(42L)).thenReturn(Optional.of(pipeline));

        service.markAsCancelled(42L);

        assertThat(pipeline.getStatus()).isEqualTo(ExportStatus.CANCELLED);
        assertThat(pipeline.getCompletedAt()).isNotNull();
        verify(pipelineRepository).save(pipeline);
    }

    @Test
    void getExportPipelineSearchRequest_returnsStoredFilter() {
        var filter = GeneSearchRequest.builder().build();
        pipeline.setFilterJson(filter);

        when(pipelineRepository.findById(42L)).thenReturn(Optional.of(pipeline));

        var result = service.getExportPipelineSearchRequest(42L);

        assertThat(result).isSameAs(filter);
    }
}
