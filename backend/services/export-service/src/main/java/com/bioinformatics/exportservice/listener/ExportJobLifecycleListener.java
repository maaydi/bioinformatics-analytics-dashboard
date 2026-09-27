package com.bioinformatics.exportservice.listener;

import com.bioinformatics.exportservice.service.ExportPipelineLifeCycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.bioinformatics.exportservice.dto.Constants.EXPORT_JOB_ID;

/**
 * Batch job lifecycle listener for export pipelines.
 *
 * <p>Hooks into Spring Batch job lifecycle to:
 * <ul>
 *   <li><strong>beforeJob:</strong> Mark pipeline as RUNNING, create execution record
 *   <li><strong>afterJob:</strong> Mark as COMPLETED/FAILED based on exit status
 * </ul>
 *
 * <p>Delegates all status updates to {@link ExportPipelineLifeCycleService}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExportJobLifecycleListener implements JobExecutionListener {

    private final ExportPipelineLifeCycleService pipelineService;

    /**
     * Called before batch job execution starts.
     *
     * <p>Updates pipeline:
     * <ul>
     *   <li>Status → RUNNING
     *   <li>Started timestamp
     *   <li>Creates ExportJobExecution record
     * </ul>
     *
     * @param jobExecution Spring Batch job execution
     */
    @Override
    public void beforeJob(@NonNull JobExecution jobExecution) {
        var pipelineId = getPipelineId(jobExecution);
        log.info("[BATCH_LISTENER] Job starting - pipelineId={}, jobExecutionId={}, startTime={}",
                pipelineId, jobExecution.getId(), jobExecution.getStartTime());
        pipelineService.markAsRunning(pipelineId, jobExecution.getId());
    }

    /**
     * Called after batch job execution completes (success or failure).
     *
     * <p>Routes to appropriate status method:
     * <ul>
     *   <li>FAILED → markAsFailed with exception messages
     *   <li>COMPLETED → markAsCompleted (file details from batch context)
     *   <li>Other → markAsFailed with generic error
     * </ul>
     *
     * @param jobExecution Spring Batch job execution (contains status and exceptions)
     */
    @Override
    public void afterJob(@NonNull JobExecution jobExecution) {
        var pipelineId = getPipelineId(jobExecution);
        var status = jobExecution.getStatus();

        log.info("[BATCH_LISTENER] Job completed - pipelineId={}, jobExecutionId={}, batchStatus={}, exitStatus={}",
                pipelineId, jobExecution.getId(), status, jobExecution.getExitStatus());

        switch (status) {
            case FAILED:
                var errorMessage = extractFailureMessage(jobExecution);
                log.error("[BATCH_LISTENER] Job failed - pipelineId={}, reason='{}'", pipelineId, errorMessage);
                pipelineService.markAsFailed(pipelineId, errorMessage);
                break;
            case COMPLETED:
                log.info("[BATCH_LISTENER] Job completed successfully - pipelineId={}", pipelineId);
                pipelineService.markAsCompleted(pipelineId, null, null, null);
                break;
            default:
                var defaultError = "Export Job did not stop properly (status: " + status + ")";
                log.error("[BATCH_LISTENER] Job ended in unexpected state - pipelineId={}, status={}", pipelineId, status);
                pipelineService.markAsFailed(pipelineId, defaultError);
        }
    }

    /**
     * Extracts pipeline ID from job parameters.
     *
     * @param jobExecution Spring Batch job execution
     * @return pipeline ID
     */
    private Long getPipelineId(JobExecution jobExecution) {
        return jobExecution
                .getJobParameters()
                .getLong(EXPORT_JOB_ID.getKey());
    }

    /**
     * Extracts human-readable failure message from job exceptions.
     *
     * @param jobExecution Spring Batch job execution (contains all exceptions)
     * @return error message or default "Export job failed"
     */
    private String extractFailureMessage(JobExecution jobExecution) {
        return jobExecution.getAllFailureExceptions()
                .stream()
                .map(Throwable::getMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Export job failed");
    }
}