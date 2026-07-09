package com.bioinformatics.dashboard.job.uniprot.apiloader;

import com.bioinformatics.dashboard.exception.ExecuteJobException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UniProtApiImportJobExecutor {

    private final JobOperator operator;
    private final Job uniProtApiImportJob;

    public UniProtApiImportJobExecutor(
            JobOperator operator,
            @Qualifier("uniProtApiImportJob") Job uniProtApiImportJob) {
        this.operator = operator;
        this.uniProtApiImportJob = uniProtApiImportJob;
    }

    @Async("importExecutor")
    public void execute(JobParameters parameters) {
        try {
            log.info("Starting remote UniProt API import job asynchronously");
            var execution = operator.start(uniProtApiImportJob, parameters);
            log.info("Remote UniProt API import job completed with status {}", execution.getExitStatus());
        } catch (Exception e) {
            log.error("Failed to execute remote UniProt API import job", e);
            throw new ExecuteJobException("Failed to start remote UniProt API import job", e);
        }
    }
}
