package com.bioinformatics.dashboard.job.uniprot.fileloader;

import com.bioinformatics.common.exception.ExecuteJobException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncUniprotImportJobExecutor {

    private final JobOperator operator;
    private final Job uniProtImportJob;

    /**
     * Asynchronously starts the UniProt import batch job using the provided {@link JobParameters}.
     *
     * <p>This method delegates to the Spring Batch {@link JobOperator} to launch the configured {@code uniProtImportJob}.
     * It is executed asynchronously (thread pool managed by Spring),
     * so callers should not expect an immediate result. Any exception during job start is wrapped in
     * {@link com.bioinformatics.dashboard.exception.ExecuteJobException} to provide a clear failure signal to callers.
     *
     * @param parameters parameters passed to the batch job; must not be null
     * @throws com.bioinformatics.dashboard.exception.ExecuteJobException when the job cannot be started
     */
    @Async("importExecutor")
    public void execute(JobParameters parameters) {
        try {
            log.info("UniProt import job started asynchronously");
            var exec = operator.start(uniProtImportJob, parameters);
            log.info("UniProt import job completed with status {}", exec.getExitStatus());
        } catch (Exception e) {
            log.error("Failed to start UniProt import job", e);
            throw new ExecuteJobException("Failed to start uniprot import job", e);
        }
    }

}
