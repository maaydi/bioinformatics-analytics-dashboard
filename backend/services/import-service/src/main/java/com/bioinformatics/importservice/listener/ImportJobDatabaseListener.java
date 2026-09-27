package com.bioinformatics.importservice.listener;

import com.bioinformatics.importservice.dto.Constants;
import com.bioinformatics.importservice.dto.ImportStatus;
import com.bioinformatics.importservice.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Batch job lifecycle listener for persisting final import results to the database.
 *
 * <p>Called after batch job completion to:
 * <ul>
 *   <li>Retrieve total records processed from all step executions
 *   <li>Calculate job duration
 *   <li>Extract error message if failed
 *   <li>Update job record with final status and metrics
 * </ul>
 *
 * <p>Delegates status transitions via {@code afterJob} hook.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ImportJobDatabaseListener implements JobExecutionListener {

    private final ImportJobRepository importJobRep;

    /**
     * Called after batch job execution completes (success or failure).
     *
     * <p>Updates import job record with:
     * <ul>
     *   <li>Final status (COMPLETED or FAILED)
     *   <li>Total records processed (sum of all step write counts)
     *   <li>Duration in milliseconds
     *   <li>Completion timestamp
     *   <li>Error message (if failed)
     * </ul>
     *
     * @param jobExecution Spring Batch job execution with final state
     */
    @Override
    public void afterJob(JobExecution jobExecution) {
        var jobId = jobExecution.getJobParameters().getString(Constants.IMPORT_JOB_ID.getKey());
        if (jobId == null) {
            log.warn("[BATCH_LISTENER] Job completed but no job ID in parameters - skipping DB update");
            return;
        }

        log.debug("[BATCH_LISTENER] Job completed - ID={}, batchStatus={}, exitStatus={}",
                jobId, jobExecution.getStatus(), jobExecution.getExitStatus());

        var importJobId = UUID.fromString(jobId);
        var jobRecord = importJobRep.findById(importJobId).orElse(null);

        if (jobRecord == null) {
            log.warn("[BATCH_LISTENER] Import job record not found - ID={}, skipping DB update", jobId);
            return;
        }

        log.debug("[BATCH_LISTENER] Calculating total records processed - ID={}", jobId);
        var totalProcessed = jobExecution.getStepExecutions()
                .stream()
                .mapToLong(StepExecution::getWriteCount)
                .sum();
        log.debug("[BATCH_LISTENER] Total records calculated - ID={}, totalProcessed={}", jobId, totalProcessed);

        assert jobExecution.getEndTime() != null;
        var durationMs = Duration.between(
                jobExecution.getCreateTime().atZone(ZoneId.systemDefault()).toInstant(),
                jobExecution.getEndTime().atZone(ZoneId.systemDefault()).toInstant()).toMillis();
        log.debug("[BATCH_LISTENER] Duration calculated - ID={}, durationMs={}ms", jobId, durationMs);

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("[BATCH_LISTENER] Job COMPLETED - ID={}, recordsProcessed={}, duration={}ms",
                    jobId, totalProcessed, durationMs);
            jobRecord.setStatus(ImportStatus.COMPLETED);
        } else {
            var errorMsg = !jobExecution.getAllFailureExceptions().isEmpty()
                    ? jobExecution.getAllFailureExceptions().getFirst().getMessage()
                    : "Import job failed";
            log.error("[BATCH_LISTENER] Job FAILED - ID={}, reason='{}', duration={}ms",
                    jobId, errorMsg, durationMs);
            jobRecord.setStatus(ImportStatus.FAILED);
            jobRecord.setErrorMessage(errorMsg);
        }

        jobRecord.setRecordsProcessed((int) totalProcessed);
        jobRecord.setEntryCount((int) totalProcessed);
        jobRecord.setDurationMs(durationMs);
        jobRecord.setCompletedAt(jobExecution.getEndTime().atZone(ZoneId.systemDefault()).toInstant());

        importJobRep.save(jobRecord);
        log.info("[BATCH_LISTENER] Import job record updated in database - ID={}, status={}, finalRecords={}",
                jobId, jobRecord.getStatus(), totalProcessed);
    }
}