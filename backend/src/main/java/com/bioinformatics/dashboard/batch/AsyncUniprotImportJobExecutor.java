package com.bioinformatics.dashboard.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.bioinformatics.dashboard.exception.ExecuteJobException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncUniprotImportJobExecutor {

    private final JobOperator operator;
    private final Job uniProtImportJob;

    @Async
    public void executeImportJob(JobParameters parameters) {
        try {
            operator.start(uniProtImportJob, parameters);
        } catch (Exception e) {
            throw new ExecuteJobException("Failed to start uniprot import job", e);
        }
    }

}
