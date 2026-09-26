package com.bioinformatics.exportservice.service;

import com.bioinformatics.common.exception.ExecuteJobException;
import com.bioinformatics.common.exception.ResourceNotFoundException;
import com.bioinformatics.common.models.PagedResponse;
import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.common.providers.DataProvider;
import com.bioinformatics.exportservice.batch.ExportJobExecutor;
import com.bioinformatics.exportservice.dto.*;
import com.bioinformatics.exportservice.entity.ExportJobExecution;
import com.bioinformatics.exportservice.entity.ExportPipeline;
import com.bioinformatics.exportservice.mapper.ExportPipelineMapper;
import com.bioinformatics.exportservice.repository.ExportJobExecutionRepository;
import com.bioinformatics.exportservice.repository.ExportPipelineRepository;
import com.bioinformatics.shared.models.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExportPipelineService {

    private final ExportPipelineRepository pipelineRepository;
    private final ExportJobExecutionRepository jobExecutionRepository;
    private final ExportPipelineMapper mapper;
    private final ExportJobExecutor executor;
    private final JobRepository jobRepository;

    public GeneSearchRequest getExportPipelineSearchRequest(Long pipelineId) {
        var pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new IllegalArgumentException("Export Pipeline with id %d not found".formatted(pipelineId)));
        return pipeline.getFilterJson();
    }

    public void updatePipelineEstimatedRows(Long pipelineId, Long estimatedRows, int chunkSize) {
        log.info("Updating estimated rows for Pipeline with id {}", pipelineId);
        pipelineRepository.findById(pipelineId)
                .ifPresentOrElse(exportPipeline -> {
                    exportPipeline.setEstimatedRows(estimatedRows);
                    pipelineRepository.save(exportPipeline);
                    jobExecutionRepository.findByPipelineId(pipelineId)
                            .ifPresentOrElse(exportJobExecution -> {
                                var estimatedChunks = (int) Math.ceil((double) estimatedRows / chunkSize);
                                exportJobExecution.setChunksTotal(estimatedChunks);
                                jobExecutionRepository.save(exportJobExecution);
                            }, () -> logFailedUpdateExecution(pipelineId));
                }, () -> logFailedUpdate(pipelineId));
    }

    public ExportPipelineResponse createPipeline(ExportPipelineCreateRequest request, UserPrincipal initiator) {
        log.info("Creating Pipeline with name {} and description {}", request.name(), request.description());
        var pipeline = mapper.toEntity(request, initiator.id());
        var result = pipelineRepository.save(pipeline);
        log.info("Creating Job Execution for pipeline {}", request.name());
        var exec = new ExportJobExecution();
        exec.setPipeline(result);
        jobExecutionRepository.save(exec);
        return executePipelineJob(result, initiator, request.fieldSchema());
    }

    public PagedResponse<ExportPipelineResponse> listPipelines(ExportStatus status, Pageable pageable, UserPrincipal user) {
        var result = pipelineRepository.findByUserIdAndStatusAndDeletedAtIsNull(user.id(), status, pageable)
                .map(mapper::toDto);
        return PagedResponse.of(result);
    }

    public ExportJobStatusResponse getPipelineStatus(Long pipelineId, UserPrincipal user) {
        var pipeline = findNotDeletedPipelineByIdAndOwner(pipelineId, user);
        var execution = jobExecutionRepository.findByPipelineId(pipelineId);
        if (execution.isPresent()) {
            var currentStep = ExportJobStepTracker.determineCurrentStep(jobRepository.getJobExecution(pipeline.getJobExecutionId()));
            return new ExportJobStatusResponse(pipelineId,
                    pipeline.getStatus(),
                    execution.get().getProgressPercent(),
                    execution.get().getChunksProcessed(),
                    execution.get().getChunksTotal(),
                    currentStep,
                    execution.get().getUpdatedAt()
            );
        } else {
            log.info("No execution found for pipeline with ID {}", pipelineId);
            return new ExportJobStatusResponse(pipelineId, pipeline.getStatus(), 0, 0, 0, null, Instant.now());
        }
    }

    public DownloadUrlDto getDownloadUrl(Long pipelineId, UserPrincipal user) throws IOException {
        var pipeline = findNotDeletedPipelineByIdAndOwner(pipelineId, user);
        if (pipeline.isCompleted()) {
            return new DownloadUrlDto("/exports/pipelines/%d/download-file".formatted(pipelineId),
                    pipeline.getFileName(), pipeline.getFileSizeBytes(), pipeline.getFormat().getContentType());
        }
        throw new NoSuchFileException("Export pipeline %s [%d] %s".formatted(pipeline.getName(), pipeline.getId(),
                pipeline.isTerminal() ? "does not complete properly." : "is still running."));

    }

    public ExportPipelineResponse retryPipeline(Long pipelineId, UserPrincipal user) {
        var pipeline = findNotDeletedPipelineByIdAndOwner(pipelineId, user);
        if (!pipeline.isTerminal()) {
            throw new ExecuteJobException("Export pipeline job %s [%d] is still running.".formatted(pipeline.getName(), pipelineId));
        }
        log.info("Retry execution for pipeline {} [{}]", pipeline.getName(), pipelineId);
        var dto = mapper.toDto(pipeline);
        var clonedPipeline = new ExportPipelineCreateRequest(dto.name(), dto.description(), dto.filter(), dto.format(), dto.fieldSchema());
        return createPipeline(clonedPipeline, user);
    }

    public void deletePipeline(Long pipelineId, UserPrincipal user) {
        var pipeline = findNotDeletedPipelineByIdAndOwner(pipelineId, user);
        if (!pipeline.isTerminal()) {
            try {
                var execution = jobRepository.getJobExecution(pipeline.getJobExecutionId());
                if (execution != null) {
                    executor.stop(execution);
                } else {
                    log.warn("No execution found for pipeline with ID {}", pipelineId);
                }
            } catch (Exception e) {
                // ignore not running exception
            }
        }
        pipeline.setDeletedAt(Instant.now());
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
                    jobExecutionRepository.findByPipelineId(pipelineId)
                            .ifPresentOrElse(jobExecution -> {
                                jobExecution.setJobExecutionId(jobExecutionId);
                                jobExecutionRepository.save(jobExecution);
                            }, () -> logFailedUpdateExecution(pipelineId));

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

    private ExportPipeline findNotDeletedPipelineByIdAndOwner(Long pipelineId, UserPrincipal user) {
        var pipeline = pipelineRepository.findByIdAndUserIdAndDeletedAtIsNull(pipelineId, user.id());
        if (pipeline.isEmpty()) {
            log.warn("Pipeline with id {} and owner by user {} not found", pipelineId, user.id());
            throw new ResourceNotFoundException("Pipeline with ID %d not found".formatted(pipelineId));
        }
        return pipeline.get();
    }

    private ExportPipelineResponse executePipelineJob(ExportPipeline pipeline, UserPrincipal initiator, List<String> fields) {
        var provider = DataProvider.isApi(initiator.dataProvider()) ? DataProvider.API : DataProvider.POSTGRES;
        try {
            var parameters = new JobParametersBuilder()
                    .addLong(Constants.EXPORT_JOB_ID.getKey(), pipeline.getId())
                    .addString(Constants.USER_ID.getKey(), initiator.id())
                    .addJobParameter(Constants.USER_ROLE.getKey(), initiator.roles(), List.class)
                    .addString(Constants.DATA_PROVIDER.getKey(), provider.getKey())
                    .addJobParameter(Constants.EXPORT_FORMAT.getKey(), pipeline.getFormat(), ExportFormat.class)
                    .addJobParameter(Constants.EXPORTED_FIELDS.getKey(), fields, List.class)
                    .toJobParameters();
            executor.execute(parameters);
            return mapper.toDto(pipeline);
        } catch (Exception e) {
            markAsFailed(pipeline.getId(), "Failed to start Export Pipeline %s <ID=%d> : %s".formatted(pipeline.getName(), pipeline.getId(), e.getMessage()));
            throw e;
        }
    }

    private void logFailedUpdate(long pipelineId) {
        log.error(
                "Failed to update Pipeline: Pipeline with ID <{}> not found",
                pipelineId
        );
    }

    private void logFailedUpdateExecution(long pipelineId) {
        log.error("Failed to update job Execution for pipeline {} : Execution not found", pipelineId);
    }
}
