package com.bioinformatics.importservice.uniprot;

import com.bioinformatics.common.exception.ExecuteJobException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.bioinformatics.importservice.dto.Constants.DATA_PROVIDER;

/**
 * Async executor for Spring Batch UniProt import jobs.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Launches batch job asynchronously via {@link JobOperator}
 *   <li>Decouples HTTP response from long-running import processing
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
public class ImportJobExecutor {

    private final JobOperator operator;
    private final Job unifiedUniProtImportJob;


    /**
     * Starts an import job asynchronously.
     *
     * <p>Flow:
     * <ol>
     *   <li>Extract data provider from parameters (FILE or API)
     *   <li>Invoke Spring Batch JobOperator
     *   <li>Job runs in separate thread pool
     *   <li>Listeners track progress and status
     * </ol>
     *
     * @param parameters job parameters (job ID, file path, provider, filter ID, etc.)
     * @throws ExecuteJobException if job fails to start
     */
    @Async("importExecutor")
    public void execute(JobParameters parameters) {
        try {
            var source = Objects.requireNonNull(parameters.getString(DATA_PROVIDER.getKey()));
            log.info("[JOB_EXECUTOR] Starting async import job - source={}", source);

            var execution = operator.start(unifiedUniProtImportJob, parameters);

            log.info("[JOB_EXECUTOR] Job started successfully - jobExecutionId={}, status={}, exitStatus={}",
                    execution.getId(), execution.getStatus(), execution.getExitStatus());
        } catch (Exception e) {
            log.error("[JOB_EXECUTOR] Failed to start import job - exception: {}", e.getMessage(), e);
            throw new ExecuteJobException("Failed to start UniProt import job", e);
        }
    }
}
