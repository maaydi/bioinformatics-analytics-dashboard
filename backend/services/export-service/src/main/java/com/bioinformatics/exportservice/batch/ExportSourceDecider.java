package com.bioinformatics.exportservice.batch;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.step.StepExecution;

import static com.bioinformatics.exportservice.dto.Constants.DATA_PROVIDER;

@Slf4j
public class ExportSourceDecider implements JobExecutionDecider {

    /**
     * Decides which step to execute based on the {@code DATA_PROVIDER} job parameter.
     *
     * @param jobExecution  the current job execution (contains job parameters)
     * @param stepExecution null (deciders are not preceded by steps)
     * @return a {@link FlowExecutionStatus} with the uppercased data provider value,
     * or "UNKNOWN" if the parameter is null
     */
    @Override
    public FlowExecutionStatus decide(JobExecution jobExecution, @Nullable StepExecution stepExecution) {
        var source = jobExecution.getJobParameters().getString(DATA_PROVIDER.getKey());
        log.info("[EXPORT] [DECIDER] Source={} jobExecution={}", source, jobExecution.getId());
        return new FlowExecutionStatus(source != null ? source : "UNKNOWN");

    }
}
