package com.bioinformatics.importservice.uniprot.apiloader;

import com.bioinformatics.common.exception.ExecuteJobException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class UniProtApiImportJobExecutor {

    private final JobOperator operator;
    private final Job uniProtApiImportJob;


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
