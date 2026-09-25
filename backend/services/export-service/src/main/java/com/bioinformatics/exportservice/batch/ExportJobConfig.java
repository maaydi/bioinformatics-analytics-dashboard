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

@Configuration
@Profile("!test")
@RequiredArgsConstructor
public class ExportJobConfig {

    private final Step uniProtApiExportStep;
    private final Step uniProtPostgresExportStep;
    private final ExportJobLifecycleListener exportJobListener;

    @Bean
    public JobExecutionDecider exportSourceDecider() {
        return new ExportSourceDecider();
    }

    @Bean
    Step validateAndEstimateStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ValidateAndEstimateTasklet tasklet) {

        return new StepBuilder(VALIDATE_ESTIMATE_TASK.getKey(), jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    Step assembleAndFinalizeStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            AssembleAndFinalizeTasklet tasklet) {

        return new StepBuilder(ASSEMBLE_FINALIZE_TASK.getKey(), jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

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
