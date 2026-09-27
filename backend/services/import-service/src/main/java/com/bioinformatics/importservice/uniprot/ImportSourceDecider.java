package com.bioinformatics.importservice.uniprot;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.step.StepExecution;

import static com.bioinformatics.importservice.dto.Constants.DATA_PROVIDER;

/**
 * Spring Batch job flow decider that routes to the appropriate import step.
 *
 * <p><b>Decision Logic:</b>
 * <ul>
 *   <li>Reads job parameter {@code DATA_PROVIDER} (set by ImportService via JobParameters)
 *   <li>Returns the parameter value (uppercased) as the flow decision status
 *   <li>Flow routing configured in ImportJobConfig
 * </ul>
 *
 * <p><b>Supported Routes:</b>
 * <ul>
 *   <li>{@code "API"} → routes to UniProt API import step (remote import)
 *   <li>{@code "FILE"} → routes to UniProt file import step (local DAT/TSV)
 *   <li>Other → "UNKNOWN" status (no matching transition, job fails gracefully)
 * </ul>
 *
 * <p><b>Design Pattern:</b> Strategy pattern — job flow is invariant,
 * actual step executed is determined by runtime parameter. Benefits:
 * <ul>
 *   <li>No code branching in job configuration
 *   <li>Easy to add new import sources
 *   <li>Source selection is data-driven
 * </ul>
 */
public class ImportSourceDecider implements JobExecutionDecider {

    /**
     * Decides which step to execute based on the DATA_PROVIDER job parameter.
     *
     * <p>Called at job flow decision point to route to API or FILE import step.
     *
     * @param jobExecution  job execution with parameters
     * @param stepExecution null (deciders not preceded by steps)
     * @return flow status (API, FILE, or UNKNOWN)
     */
    @Override
    public FlowExecutionStatus decide(JobExecution jobExecution, @Nullable StepExecution stepExecution) {
        var source = jobExecution.getJobParameters().getString(DATA_PROVIDER.getKey());
        var decision = source != null ? source.toUpperCase() : "UNKNOWN";
        return new FlowExecutionStatus(decision);
    }
}
