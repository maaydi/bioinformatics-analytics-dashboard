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
 * Spring Batch job configuration for unified UniProt data imports.
 *
 * <p>This configuration orchestrates two import paths via runtime decision:
 * <ul>
 *   <li><b>API-based import:</b> Fetches protein data from UniProt REST API ({@code DATA_PROVIDER = "api"})
 *   <li><b>File-based import:</b> Reads from uploaded .dat or .tsv file ({@code DATA_PROVIDER = "file"})
 * </ul>
 *
 * <p><b>Job Flow:</b>
 * <pre>
 * importSourceDecider
 *   ├→ [API]  → uniProtApiImportStep
 *   └→ [FILE] → uniProtImportStep
 *   ↓
 * Global Listeners (applied to all paths):
 *   ├→ ImportJobDatabaseListener (persist final status/counts)
 *   ├→ PostImportCacheEvictionListener (clear caches)
 *   └→ ImportJobRefreshViewsListener (rebuild analytical views)
 * </pre>
 *
 * <p><b>Advantages of Unified Design:</b>
 * <ul>
 *   <li>Single job definition serves both sources — no code duplication
 *   <li>Source selection is a runtime parameter, not compile-time branch
 *   <li>Listeners apply uniformly to all sources
 *   <li>Easy to add new import sources: create step config, add decision path
 * </ul>
 *
 * <p><b>Step Implementations:</b>
 * <ul>
 *   <li>API: {@code com.bioinformatics.importservice.uniprot.apiloader.UniProtApiImportJobConfig}
 *   <li>File: {@code com.bioinformatics.importservice.uniprot.fileloader.UniProtImportJobConfig}
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
     * <p>Reads DATA_PROVIDER parameter and returns decision status for flow routing.
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
     * <p><b>Job Flow:</b>
     * <ol>
     *   <li>Job starts with DATA_PROVIDER parameter ("api" or "file")
     *   <li>Execute importSourceDecider to read and route parameter
     *   <li>If "API" → execute uniProtApiImportStep
     *   <li>If "FILE" → execute uniProtImportStep
     *   <li>After step completion, apply global listeners:
     *       <ul>
     *         <li>DatabaseListener: persist final status/metrics
     *         <li>CacheEvictionListener: clear all caches
     *         <li>RefreshViewsListener: rebuild materialized views
     *       </ul>
     * </ol>
     *
     * <p>All listeners are executed after either step, ensuring
     * consistent post-import behavior regardless of source.
     *
     * @param jobRepository the Spring Batch job repository
     * @return a configured {@link Job} implementing unified import orchestration
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
