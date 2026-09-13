package com.bioinformatics.importservice.uniprot;

import com.bioinformatics.importservice.dto.Constants;
import com.bioinformatics.importservice.listener.ImportJobDatabaseListener;
import com.bioinformatics.importservice.listener.ImportJobRefreshViewsListener;
import com.bioinformatics.importservice.listener.PostImportCacheEvictionListener;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Unified Spring Batch job configuration for UniProt imports.
 *
 * <p>This configuration orchestrates two import paths via a runtime decision:
 * <ul>
 *   <li><b>API-based import:</b> fetches protein data from UniProt REST API ({@code DATA_PROVIDER = "api"})</li>
 *   <li><b>File-based import:</b> reads from uploaded .dat or .tsv file ({@code DATA_PROVIDER = "file"})</li>
 * </ul>
 *
 * <p><b>Flow:</b> The job reads the {@code DATA_PROVIDER} job parameter, runs it through
 * {@link ImportSourceDecider}, and routes to the appropriate step ({@code uniProtApiImportStep} or
 * {@code uniProtImportStep}). Both steps share the same post-processing logic (database refresh,
 * cache eviction, audit logging) via global listeners.
 *
 * <p><b>Advantages:</b>
 * <ul>
 *   <li>Single job definition serves both sources — no code duplication</li>
 *   <li>Source selection is a runtime parameter, not a compile-time branch</li>
 *   <li>Easy to add new import sources: create a step config, add a decision path</li>
 *   <li>Listeners apply uniformly to all sources</li>
 * </ul>
 *
 * <p><b>Step implementations:</b>
 * <ul>
 *   <li>{@link com.bioinformatics.importservice.uniprot.apiloader.UniProtApiImportJobConfig} —
 *       defines {@code uniProtApiImportStep}</li>
 *   <li>{@link com.bioinformatics.importservice.uniprot.fileloader.UniProtImportJobConfig} —
 *       defines {@code uniProtImportStep}</li>
 * </ul>
 */
@Configuration
@Profile("!test")
@RequiredArgsConstructor
public class ImportJobConfig {

    private final Step uniProtApiImportStep;
    private final Step uniProtImportStep;
    private final ImportJobDatabaseListener databaseListener;
    private final PostImportCacheEvictionListener cacheEvictionListener;
    private final ImportJobRefreshViewsListener refreshViewsListener;

    /**
     * Creates the job execution decider that routes to API or file-based import steps.
     *
     * @return a new {@link ImportSourceDecider} instance
     */
    @Bean
    public JobExecutionDecider importSourceDecider() {
        return new ImportSourceDecider();
    }

    /**
     * Defines the unified UniProt import job with conditional step routing.
     *
     * <p>Job flow:
     * <ol>
     *   <li>Launch job with job parameter {@code DATA_PROVIDER} set to "api" or "file"</li>
     *   <li>Execute {@link #importSourceDecider()} to read and uppercase the parameter</li>
     *   <li>Route to {@code uniProtApiImportStep} if decision is "API", or {@code uniProtImportStep} if "FILE"</li>
     *   <li>Apply post-step listeners: database refresh, cache eviction, audit logging</li>
     * </ol>
     *
     * @param jobRepository the Spring Batch job repository
     * @return a configured {@link Job} that implements unified import orchestration
     */
    @Bean
    public Job unifiedUniProtImportJob(JobRepository jobRepository) {
        return new JobBuilder(Constants.UNIPROT_IMPORT_JOB.getKey(), jobRepository)
                .listener(databaseListener)
                .listener(cacheEvictionListener)
                .listener(refreshViewsListener)
                .start(importSourceDecider())
                .on(Constants.API.getKey()).to(uniProtApiImportStep)
                .from(importSourceDecider())
                .on(Constants.FILE.getKey()).to(uniProtImportStep)
                .end()
                .build();
    }

}
