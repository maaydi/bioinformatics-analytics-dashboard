package com.bioinformatics.exportservice.service;

import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.exportservice.dto.ExportStatus;
import com.bioinformatics.exportservice.entity.ExportPipeline;
import com.bioinformatics.exportservice.repository.ExportJobExecutionRepository;
import com.bioinformatics.exportservice.repository.ExportPipelineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExportPipelineTaskletService {

    private final ExportPipelineRepository pipelineRepository;
    private final ExportJobExecutionRepository jobExecutionRepository;

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

    private void logFailedUpdateExecution(long pipelineId) {
        log.error("Failed to update job Execution for pipeline {} : Execution not found", pipelineId);
    }
}
