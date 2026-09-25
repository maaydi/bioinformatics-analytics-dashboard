package com.bioinformatics.exportservice.service;

import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.common.providers.DataProvider;
import com.bioinformatics.exportservice.batch.ExportJobExecutor;
import com.bioinformatics.exportservice.dto.*;
import com.bioinformatics.exportservice.entity.ExportPipeline;
import com.bioinformatics.exportservice.mapper.ExportPipelineMapper;
import com.bioinformatics.exportservice.repository.ExportPipelineRepository;
import com.bioinformatics.shared.models.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExportPipelineService {

    private final ExportPipelineRepository pipelineRepository;
    private final ExportPipelineMapper mapper;
    private final ExportJobExecutor executor;

    public GeneSearchRequest getExportPipelineSearchRequest(Long pipelineId) {
        var pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new IllegalArgumentException("Export Pipeline with id %d not found".formatted(pipelineId)));
        return pipeline.getFilterJson();
    }

    public void updatePipelineEstimatedRows(Long pipelineId, Long estimatedRows) {
        log.info("Updating estimated rows for Pipeline with id {}", pipelineId);
        pipelineRepository.findById(pipelineId)
                .ifPresentOrElse(exportPipeline -> {
                    exportPipeline.setEstimatedRows(estimatedRows);
                    pipelineRepository.save(exportPipeline);
                }, () -> logFailedUpdate(pipelineId));
    }

    public ExportPipelineResponse createPipeline(ExportPipelineCreateRequest request, UserPrincipal initiator) {
        log.info("Creating Pipeline with name {} and description {}", request.name(), request.description());
        var pipeline = mapper.toEntity(request, initiator.id());
        var result = pipelineRepository.save(pipeline);
        var provider = DataProvider.isApi(initiator.dataProvider()) ? DataProvider.API : DataProvider.POSTGRES;
        try {
            var parameters = new JobParametersBuilder()
                    .addLong(Constants.EXPORT_JOB_ID.getKey(), result.getId())
                    .addString(Constants.USER_ID.getKey(), initiator.id())
                    .addJobParameter(Constants.USER_ROLE.getKey(), initiator.roles(), List.class)
                    .addString(Constants.DATA_PROVIDER.getKey(), provider.getKey())
                    .addJobParameter(Constants.EXPORT_FORMAT.getKey(), request.format(), ExportFormat.class)
                    .addJobParameter(Constants.EXPORTED_FIELDS.getKey(), request.fieldSchema(), List.class)
                    .toJobParameters();
            executor.execute(parameters);
            return mapper.toDto(result);
        } catch (Exception e) {
            markAsFailed(result.getId(), "Failed to start Export Pipeline %s <ID=%d> : %s".formatted(pipeline.getName(), pipeline.getId(), e.getMessage()));
            throw e;
        }
    }

    public void markAsRunning(
            Long pipelineId,
            Long jobExecutionId) {
        log.info("Marking Pipeline as Running for Pipeline with id {}", pipelineId);

        pipelineRepository.findById(pipelineId)
                .ifPresentOrElse(pipeline -> {

                    pipeline.setStatus(ExportStatus.RUNNING);
                    pipeline.setJobExecutionId(jobExecutionId);
                    pipeline.setStartedAt(Instant.now());

                    pipelineRepository.save(pipeline);

                    log.info(
                            "Pipeline {} started with JobExecution {}",
                            pipelineId,
                            jobExecutionId
                    );

                }, () -> logFailedUpdate(pipelineId));
    }

    public void markAsCompleted(
            Long pipelineId,
            String finalFile,
            Long fileSizeBytes,
            Long actualRows) {
        log.info("Marking Pipeline as Completed for Pipeline with id {}", pipelineId);

        pipelineRepository.findById(pipelineId)
                .ifPresentOrElse(pipeline -> {

                    pipeline.setStatus(ExportStatus.COMPLETED);
                    pipeline.setFilePath(Objects.requireNonNullElse(finalFile, pipeline.getFilePath()));
                    pipeline.setFileSizeBytes(Objects.requireNonNullElse(fileSizeBytes, pipeline.getFileSizeBytes()));
                    pipeline.setActualRows(Objects.requireNonNullElse(actualRows, pipeline.getActualRows()));

                    completeTiming(pipeline);

                    pipelineRepository.save(pipeline);

                    log.info(
                            "Pipeline {} completed successfully",
                            pipelineId
                    );

                }, () -> logFailedUpdate(pipelineId));
    }

    public void markAsFailed(
            Long pipelineId,
            String errorMessage) {
        log.info("Marking Pipeline as Failed for Pipeline with id {}, Raison: {}", pipelineId, errorMessage);

        pipelineRepository.findById(pipelineId)
                .ifPresentOrElse(pipeline -> {

                    pipeline.setStatus(ExportStatus.FAILED);
                    pipeline.setErrorMessage(errorMessage);

                    completeTiming(pipeline);

                    pipelineRepository.save(pipeline);

                    log.error(
                            "Pipeline {} failed: {}",
                            pipelineId,
                            errorMessage
                    );

                }, () -> logFailedUpdate(pipelineId));
    }

    public void markAsCancelled(Long pipelineId) {
        log.info("Marking Pipeline as cancelled for Pipeline with id {}", pipelineId);
        pipelineRepository.findById(pipelineId)
                .ifPresentOrElse(pipeline -> {

                    pipeline.setStatus(ExportStatus.CANCELLED);

                    completeTiming(pipeline);

                    pipelineRepository.save(pipeline);

                }, () -> logFailedUpdate(pipelineId));
    }

    private void completeTiming(ExportPipeline pipeline) {

        var completedAt = Instant.now();

        pipeline.setCompletedAt(completedAt);

        if (pipeline.getStartedAt() != null) {
            pipeline.setDurationMs(
                    completedAt.toEpochMilli()
                            - pipeline.getStartedAt().toEpochMilli()
            );
        }
    }

    private void logFailedUpdate(long pipelineId) {
        log.error(
                "Failed to update Pipeline: Pipeline with ID <{}> not found",
                pipelineId
        );
    }
}
