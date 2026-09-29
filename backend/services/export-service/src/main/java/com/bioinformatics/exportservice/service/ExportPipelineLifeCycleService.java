package com.bioinformatics.exportservice.service;

import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.exportservice.dto.ExportStatus;
import com.bioinformatics.exportservice.entity.ExportJobExecution;
import com.bioinformatics.exportservice.entity.ExportPipeline;
import com.bioinformatics.exportservice.repository.ExportJobExecutionRepository;
import com.bioinformatics.exportservice.repository.ExportPipelineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

/**
 * Manages the lifecycle and status updates of export pipelines.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Pipeline status transitions (QUEUED → RUNNING → COMPLETED/FAILED/CANCELLED)
 *   <li>Progress tracking (estimated rows, chunk counts)
 *   <li>Duration calculation
 *   <li>Filter criteria retrieval
 * </ul>
 *
 * <p>Called by:
 * <ul>
 *   <li>ExportJobLifecycleListener (status transitions)
 *   <li>ValidateAndEstimateTasklet (row estimation)
 *   <li>AssembleAndFinalizeTasklet (completion)
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExportPipelineLifeCycleService {

    private final ExportPipelineRepository pipelineRepository;
    private final ExportJobExecutionRepository jobExecutionRepository;

    /**
     * Marks a pipeline as COMPLETED with final file metadata.
     *
     * <p>Updates:
     * <ul>
     *   <li>Status → COMPLETED
     *   <li>File path and size
     *   <li>Actual rows processed
     *   <li>Duration (completedAt - startedAt)
     * </ul>
     *
     * @param pipelineId    pipeline identifier
     * @param finalFile     absolute file path (null preserves existing)
     * @param fileSizeBytes file size in bytes (null preserves existing)
     * @param actualRows    rows written to file (null preserves existing)
     */
    public void markAsCompleted(
            Long pipelineId,
            String finalFile,
            Long fileSizeBytes,
            Long actualRows) {
        log.info("[LIFECYCLE] Marking COMPLETED - ID={}, finalFile={}, size={} bytes, rows={}",
                pipelineId, finalFile, fileSizeBytes, actualRows);

        pipelineRepository.findById(pipelineId)
                .ifPresentOrElse(pipeline -> {

                    pipeline.setStatus(ExportStatus.COMPLETED);
                    pipeline.setFilePath(Objects.requireNonNullElse(finalFile, pipeline.getFilePath()));
                    pipeline.setFileSizeBytes(Objects.requireNonNullElse(fileSizeBytes, pipeline.getFileSizeBytes()));
                    pipeline.setActualRows(Objects.requireNonNullElse(actualRows, pipeline.getActualRows()));

                    completeTiming(pipeline);

                    pipelineRepository.save(pipeline);

                    log.info("[LIFECYCLE] Pipeline COMPLETED - ID={}, finalFile={}, duration={}ms",
                            pipelineId, pipeline.getFilePath(), pipeline.getDurationMs());

                }, () -> logFailedUpdate(pipelineId));
    }

    /**
     * Retrieves the search filter criteria for a pipeline export.
     *
     * <p>Used by batch job to fetch data according to the stored filter.
     *
     * @param pipelineId pipeline identifier
     * @return gene search request/filter
     * @throws IllegalArgumentException if pipeline not found
     */
    public GeneSearchRequest getExportPipelineSearchRequest(Long pipelineId) {
        log.debug("[LIFECYCLE] Retrieving filter for pipeline - ID={}", pipelineId);
        var pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> {
                    log.error("[LIFECYCLE] Pipeline not found - ID={}", pipelineId);
                    return new IllegalArgumentException("Export Pipeline with id %d not found".formatted(pipelineId));
                });
        log.debug("[LIFECYCLE] Filter retrieved - ID={}", pipelineId);
        return pipeline.getFilterJson();
    }

    /**
     * Updates estimated row count and recalculates chunk count.
     *
     * <p>Called during validation phase to inform UI and chunk processing.
     *
     * @param pipelineId   pipeline identifier
     * @param estimatedRows total rows matching filter
     * @param chunkSize    batch size per chunk
     */
    public void updatePipelineEstimatedRows(Long pipelineId, Long estimatedRows, int chunkSize) {
        log.info("[LIFECYCLE] Updating estimated rows - ID={}, estimatedRows={}, chunkSize={}",
                pipelineId, estimatedRows, chunkSize);

        pipelineRepository.findById(pipelineId)
                .ifPresentOrElse(exportPipeline -> {
                    exportPipeline.setEstimatedRows(estimatedRows);
                    pipelineRepository.save(exportPipeline);
                    log.debug("[LIFECYCLE] Pipeline estimated rows saved - ID={}, estimatedRows={}", pipelineId, estimatedRows);

                    jobExecutionRepository.findByPipelineId(pipelineId)
                            .ifPresentOrElse(exportJobExecution -> {
                                var estimatedChunks = (int) Math.ceil((double) estimatedRows / chunkSize);
                                exportJobExecution.setChunksTotal(estimatedChunks);
                                jobExecutionRepository.save(exportJobExecution);
                                log.info("[LIFECYCLE] Job execution updated - ID={}, chunksTotal={}", pipelineId, estimatedChunks);
                            }, () -> logFailedUpdateExecution(pipelineId));
                }, () -> logFailedUpdate(pipelineId));
    }

    /**
     * Marks a pipeline as RUNNING when batch job starts.
     *
     * <p>Updates:
     * <ul>
     *   <li>Status → RUNNING
     *   <li>Batch job execution ID reference
     *   <li>Started timestamp
     *   <li>Creates ExportJobExecution record
     * </ul>
     *
     * @param pipelineId    pipeline identifier
     * @param jobExecutionId Spring Batch job execution ID
     */
    public void markAsRunning(
            Long pipelineId,
            Long jobExecutionId) {
        log.info("[LIFECYCLE] Marking RUNNING - ID={}, batchJobExecutionId={}",
                pipelineId, jobExecutionId);

        pipelineRepository.findById(pipelineId)
                .ifPresentOrElse(pipeline -> {

                    pipeline.setStatus(ExportStatus.RUNNING);
                    pipeline.setJobExecutionId(jobExecutionId);
                    pipeline.setStartedAt(Instant.now());
                    pipelineRepository.save(pipeline);
                    log.debug("[LIFECYCLE] Pipeline saved as RUNNING - ID={}, startedAt={}", pipelineId, pipeline.getStartedAt());

                    var exec = new ExportJobExecution();
                    exec.setPipeline(pipeline);
                    exec.setJobExecutionId(jobExecutionId);
                    jobExecutionRepository.save(exec);

                    log.info("[LIFECYCLE] Job execution created - ID={}, jobExecutionId={}, pipelineId={}",
                            exec.getId(), jobExecutionId, pipelineId);

                }, () -> logFailedUpdate(pipelineId));
    }

    /**
     * Marks a pipeline as FAILED with error details.
     *
     * <p>Updates:
     * <ul>
     *   <li>Status → FAILED
     *   <li>Error message (for UI display/debugging)
     *   <li>Completion timestamp and duration
     * </ul>
     *
     * @param pipelineId   pipeline identifier
     * @param errorMessage description of failure cause
     */
    public void markAsFailed(
            Long pipelineId,
            String errorMessage) {
        log.error("[LIFECYCLE] Marking FAILED - ID={}, reason='{}'", pipelineId, errorMessage);

        pipelineRepository.findById(pipelineId)
                .ifPresentOrElse(pipeline -> {

                    pipeline.setStatus(ExportStatus.FAILED);
                    pipeline.setErrorMessage(errorMessage);

                    completeTiming(pipeline);

                    pipelineRepository.save(pipeline);

                    log.error("[LIFECYCLE] Pipeline FAILED - ID={}, duration={}ms, error='{}'",
                            pipelineId, pipeline.getDurationMs(), errorMessage);

                }, () -> logFailedUpdate(pipelineId));
    }

    /**
     * Marks a pipeline as CANCELLED (user or system initiated).
     *
     * <p>Updates:
     * <ul>
     *   <li>Status → CANCELLED
     *   <li>Completion timestamp and duration
     * </ul>
     *
     * @param pipelineId pipeline identifier
     */
    public void markAsCancelled(Long pipelineId) {
        log.info("[LIFECYCLE] Marking CANCELLED - ID={}", pipelineId);

        pipelineRepository.findById(pipelineId)
                .ifPresentOrElse(pipeline -> {

                    pipeline.setStatus(ExportStatus.CANCELLED);

                    completeTiming(pipeline);

                    pipelineRepository.save(pipeline);

                    log.info("[LIFECYCLE] Pipeline CANCELLED - ID={}, duration={}ms", pipelineId, pipeline.getDurationMs());

                }, () -> logFailedUpdate(pipelineId));
    }

    /**
     * Calculates and sets completion timing metrics.
     *
     * @param pipeline pipeline entity to update
     */
    private void completeTiming(ExportPipeline pipeline) {
        var completedAt = Instant.now();
        pipeline.setCompletedAt(completedAt);

        if (pipeline.getStartedAt() != null) {
            pipeline.setDurationMs(
                    completedAt.toEpochMilli()
                            - pipeline.getStartedAt().toEpochMilli()
            );
            log.debug("[LIFECYCLE] Timing calculated - duration={}ms", pipeline.getDurationMs());
        }
    }

    /**
     * Logs when a pipeline update fails because the record is not found.
     */
    private void logFailedUpdate(long pipelineId) {
        log.error("[LIFECYCLE] Failed to update pipeline - ID={} not found in database", pipelineId);
    }

    /**
     * Logs when a job execution update fails.
     */
    private void logFailedUpdateExecution(long pipelineId) {
        log.error("[LIFECYCLE] Failed to update job execution - no execution record found for pipeline ID={}", pipelineId);
    }
}
