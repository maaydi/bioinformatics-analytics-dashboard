package com.bioinformatics.exportservice.batch;

import com.bioinformatics.common.exception.ExecuteJobException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.bioinformatics.exportservice.dto.Constants.DATA_PROVIDER;
import static com.bioinformatics.exportservice.dto.Constants.EXPORT_JOB_ID;


/**
 * Async executor for Spring Batch export jobs.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Launches batch job asynchronously via {@link JobOperator}
 *   <li>Handles job stopping/cancellation
 *   <li>Translates batch errors to domain exceptions
 * </ul>
 *
 * <p>The {@code @Async} annotation ensures job startup doesn't block the HTTP response.
 * Job progress is tracked via listeners and database updates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class ExportJobExecutor {

    private final JobOperator operator;
    private final Job exportPipelineJob;


    /**
     * Starts an export job asynchronously.
     *
     * <p>Flow:
     * <ol>
     *   <li>Extracts data provider from parameters
     *   <li>Invokes Spring Batch JobOperator
     *   <li>Job runs in separate thread pool
     *   <li>Listeners track progress and status
     * </ol>
     *
     * @param parameters job parameters (pipeline ID, user, format, fields, etc.)
     * @throws ExecuteJobException if job fails to start
     */
    @Async("exportExecutor")
    public void execute(JobParameters parameters) {
        try {
            var source = Objects.requireNonNull(parameters.getString(DATA_PROVIDER.getKey()));
            log.info("[JOB_EXECUTOR] Starting async export job - source={}", source);

            var execution = operator.start(exportPipelineJob, parameters);
            log.info("[JOB_EXECUTOR] Job started successfully - jobExecutionId={}, status={}, exitStatus={}",
                    execution.getId(), execution.getStatus(), execution.getExitStatus());
            log.info("[JOB_EXECUTOR] Export job from {} completed with status {}", source, execution.getExitStatus());
        } catch (Exception e) {
            log.error("[JOB_EXECUTOR] Failed to start export job - exception: {}", e.getMessage(), e);
            throw new ExecuteJobException("Failed to start UniProt export job", e);
        }
    }

    /**
     * Stops a running export job asynchronously.
     *
     * <p>Gracefully terminates:
     * <ul>
     *   <li>Current step processing
     *   <li>Chunk iteration
     *   <li>Updates status to STOPPED
     * </ul>
     *
     * @param jobExecution Spring Batch job execution to stop
     * @throws ExecuteJobException if job is not running or stop fails
     */
    @Async("exportExecutor")
    public void stop(JobExecution jobExecution) {
        var id = jobExecution.getJobParameters().getLong(EXPORT_JOB_ID.getKey());
        try {
            log.info("[JOB_EXECUTOR] Stopping job - pipelineId={}, jobExecutionId={}, currentStatus={}",
                    id, jobExecution.getId(), jobExecution.getStatus());

            operator.stop(jobExecution);

            log.info("[JOB_EXECUTOR] Job stopped successfully - pipelineId={}, jobExecutionId={}", id, jobExecution.getId());
        } catch (JobExecutionNotRunningException e) {
            log.warn("[JOB_EXECUTOR] Cannot stop job - not running - pipelineId={}, jobExecutionId={}",
                    id, jobExecution.getId());
            throw new ExecuteJobException("Failed to stop UniProt Export job", e);
        } catch (Exception e) {
            log.error("[JOB_EXECUTOR] Error stopping job - pipelineId={}, jobExecutionId={}, exception: {}",
                    id, jobExecution.getId(), e.getMessage(), e);
            throw new ExecuteJobException("Failed to stop UniProt Export job", e);
        }
    }
}
