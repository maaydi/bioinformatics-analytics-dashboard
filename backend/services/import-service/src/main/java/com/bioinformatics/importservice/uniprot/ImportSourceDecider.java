package com.bioinformatics.importservice.uniprot;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.step.StepExecution;

import static com.bioinformatics.importservice.dto.Constants.DATA_PROVIDER;

/**
 * Spring Batch decider that routes UniProt import jobs to the appropriate step based on data source.
 *
 * <p><b>Decision Logic:</b>
 * <ul>
 *   <li>Reads job parameter {@code DATA_PROVIDER} (set by caller via {@code JobParameters})</li>
 *   <li>Returns the parameter value (uppercased) as the flow decision status</li>
 *   <li>Flow routing is configured in {@link ImportJobConfig#unifiedUniProtImportJob(org.springframework.batch.core.repository.JobRepository)}</li>
 * </ul>
 *
 * <p><b>Supported Values:</b>
 * <ul>
 *   <li>{@code "api"} → routes to {@code uniProtApiImportStep}</li>
 *   <li>{@code "file"} → routes to {@code uniProtImportStep}</li>
 *   <li>Any other value → results in status "UNKNOWN" (no matching transition, job fails gracefully)</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>
 * JobParameters params = new JobParametersBuilder()
 *     .addString(Constants.DATA_PROVIDER.getKey(), "api")
 *     .toJobParameters();
 * JobExecution exec = jobLauncher.run(unifiedUniProtImportJob, params);
 * </pre>
 *
 * <p><b>Design Pattern:</b> This decider implements the Strategy pattern — the job flow is invariant,
 * but the actual step executed is determined by the runtime parameter. This allows:
 * <ul>
 *   <li>No code branching in the job definition itself</li>
 *   <li>Easy addition of new sources (add decision path, create step config)</li>
 *   <li>Testability: source selection is data-driven, not hard-coded</li>
 * </ul>
 */
public class ImportSourceDecider implements JobExecutionDecider {

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
        return new FlowExecutionStatus(source != null ? source.toUpperCase() : "UNKNOWN");

    }
}
