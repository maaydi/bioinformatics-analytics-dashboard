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


@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class ExportJobExecutor {

    private final JobOperator operator;
    private final Job exportPipelineJob;


    @Async("importExecutor")
    public void execute(JobParameters parameters) {
        try {
            var source = Objects.requireNonNull(parameters.getString(DATA_PROVIDER.getKey()));
            log.info("Starting UniProt Export job from {} asynchronously", source);
            var execution = operator.start(exportPipelineJob, parameters);
            log.info("UniProt export job from {} completed with status {}", source, execution.getExitStatus());
        } catch (Exception e) {
            log.error("Failed to start UniProt export job", e);
            throw new ExecuteJobException("Failed to start UniProt export job", e);
        }
    }

    public void stop(JobExecution jobExecution) {
        var id = jobExecution.getJobParameters().getLong(EXPORT_JOB_ID.getKey());
        try {
            log.info("Stop UniProt Export job pipeline with ID {}, Execution ID {}", id, jobExecution.getId());
            operator.stop(jobExecution);
        } catch (JobExecutionNotRunningException e) {
            log.error("Failed to stop UniProt Export job", e);
            throw new ExecuteJobException("Failed to stop UniProt Export job", e);
        }
    }
}
