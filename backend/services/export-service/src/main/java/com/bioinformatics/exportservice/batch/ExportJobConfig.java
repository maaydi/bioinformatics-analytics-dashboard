package com.bioinformatics.exportservice.batch;

import com.bioinformatics.common.providers.DataProvider;
import com.bioinformatics.exportservice.dto.Constants;
import com.bioinformatics.exportservice.listener.ExportJobLifecycleListener;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;

import static com.bioinformatics.exportservice.dto.Constants.ASSEMBLE_FINALIZE_TASK;
import static com.bioinformatics.exportservice.dto.Constants.VALIDATE_ESTIMATE_TASK;

/**
 * Spring Batch job configuration for export pipelines.
 *
 * <p>Defines the job flow:
 * <ol>
 *   <li><strong>validate-and-estimate</strong> - Count rows and calculate chunks
 *   <li><strong>export-source-decider</strong> - Route to API or Postgres step
 *   <li><strong>uniprot-api-export-step</strong> or <strong>uniprot-postgres-export-step</strong> - Read/write data
 *   <li><strong>assemble-and-finalize</strong> - Merge segments and mark complete
 * </ol>
 *
 * <p>Listeners:
 * <ul>
 *   <li>{@link ExportJobLifecycleListener} - Lifecycle hooks (beforeJob, afterJob)
 * </ul>
 *
 * <p>Profiles: Active when not testing (profile != "test").
 *
 * @see ExportJobParameters
 * @see ExportSourceDecider
 */
@Configuration
@Profile("!test")
@RequiredArgsConstructor
public class ExportJobConfig {

    private final Step uniProtApiExportStep;
    private final Step uniProtPostgresExportStep;
    private final ExportJobLifecycleListener exportJobListener;

    /**
     * Decider to route job to correct data source (API vs. Postgres).
     *
     * @return job execution decider
     */
    @Bean
    public JobExecutionDecider exportSourceDecider() {
        return new ExportSourceDecider();
    }

    /**
     * Step for validating filter and estimating row count.
     *
     * @param jobRepository      job repository
     * @param transactionManager transaction manager
     * @param tasklet            validation tasklet
     * @return configured step
     */
    @Bean
    Step validateAndEstimateStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ValidateAndEstimateTasklet tasklet) {

        return new StepBuilder(VALIDATE_ESTIMATE_TASK.getKey(), jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    /**
     * Step for assembling segments and finalizing export file.
     *
     * @param jobRepository job repository
     * @param transactionManager transaction manager
     * @param tasklet assembly tasklet
     * @return configured step
     */
    @Bean
    Step assembleAndFinalizeStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            AssembleAndFinalizeTasklet tasklet) {

        return new StepBuilder(ASSEMBLE_FINALIZE_TASK.getKey(), jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    /**
     * Main export pipeline job.
     *
     * <p>Flow:
     * <pre>
     * validate-and-estimate
     *   ↓
     * export-source-decider
     *   ├→ [API] → uniprot-api-export-step → assemble-and-finalize → END
     *   └→ [DB]  → uniprot-postgres-export-step → assemble-and-finalize → END
     * </pre>
     *
     * @param jobRepository job repository
     * @param validateAndEstimateStep validation step
     * @param assembleAndFinalizeStep assembly step
     * @return configured job
     */
    @Bean
    Job exportPipelineJob(JobRepository jobRepository, Step validateAndEstimateStep, Step assembleAndFinalizeStep) {

        return new JobBuilder(Constants.UNIPROT_EXPORT_JOB.getKey(), jobRepository)
                .listener(exportJobListener)
                .start(validateAndEstimateStep)
                .next(exportSourceDecider())
                .on(DataProvider.API.getKey())
                .to(uniProtApiExportStep)
                .next(assembleAndFinalizeStep)
                .from(exportSourceDecider())
                .on(DataProvider.POSTGRES.getKey())
                .to(uniProtPostgresExportStep)
                .next(assembleAndFinalizeStep)
                .end()
                .build();

    }
}
