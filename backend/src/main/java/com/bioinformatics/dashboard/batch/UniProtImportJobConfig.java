package com.bioinformatics.dashboard.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch configuration for the UniProt import pipeline.
 *
 * <p>Pipeline overview (documentation/overview.md §14):
 * <ol>
 *   <li><b>Reader</b>  — reads records from .dat or .tsv file in chunks of {@code BATCH_CHUNK_SIZE}</li>
 *   <li><b>Processor</b> — validates each record (validation-rules.md §1 + §3), maps to entity</li>
 *   <li><b>Writer</b>  — upserts to {@code protein_entry} with Overwrite strategy</li>
 *   <li><b>Post-step</b> — refreshes all materialized views</li>
 * </ol>
 *
 * <p>Transaction boundary: one database transaction per chunk (chunk-size = 500).
 * A chunk failure rolls back only that chunk (overview.md §14.3).
 *
 * <p>Concurrency: only one job may run at a time; enforced by {@code ImportService}.
 */
@Configuration
@org.springframework.context.annotation.Profile("!test")
public class UniProtImportJobConfig {

    // TODO: inject UniProtItemReader, UniProtItemProcessor, UniProtItemWriter, chunkSize

    /**
     * Main import job bean.
     * Triggered by POST /api/admin/import/uniprot (not on application startup).
     */
    @Bean
    public Job uniProtImportJob(JobRepository jobRepository, Step uniProtImportStep) {
        return new JobBuilder("uniProtImportJob", jobRepository)
                .start(uniProtImportStep)
                // TODO: add post-import step to REFRESH MATERIALIZED VIEW CONCURRENTLY
                .build();
    }

    @Bean
    public Step uniProtImportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {
        return new StepBuilder("uniProtImportStep", jobRepository)
                // TODO: configure reader, processor, writer, chunk size, skip policy
                .<Object, Object>chunk(500, transactionManager)
                .reader(null)      // TODO: replace with UniProtItemReader
                .processor(null)   // TODO: replace with UniProtItemProcessor
                .writer(null)      // TODO: replace with UniProtItemWriter
                .build();
    }
}
