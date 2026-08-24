package com.bioinformatics.dashboard.job.uniprot.fileloader;

import com.bioinformatics.common.exception.MalformedUniprotFileException;
import com.bioinformatics.common.gene.entity.ProteinEntry;
import com.bioinformatics.dashboard.config.AppProperties;
import com.bioinformatics.dashboard.job.dto.Constants;
import com.bioinformatics.dashboard.job.listener.*;
import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.ProteinEntryItemProcessor;
import com.bioinformatics.dashboard.job.uniprot.fileloader.reader.DelegatingItemStreamReader;
import com.bioinformatics.dashboard.job.uniprot.fileloader.reader.UniprotDatItemReader;
import com.bioinformatics.dashboard.job.writer.ProteinAggregateItemWriter;
import lombok.RequiredArgsConstructor;
import org.hibernate.StaleObjectStateException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch configuration for the UniProt import pipeline.
 *
 * <p>
 * Pipeline overview (documentation/overview.md §14):
 * <ol>
 * <li><b>Reader</b> — reads records from .dat or .tsv file in chunks of
 * {@code BATCH_CHUNK_SIZE}</li>
 * <li><b>Processor</b> — validates each record (validation-rules.md §1 + §3),
 * maps to entity</li>
 * <li><b>Writer</b> — upserts to {@code protein_entry} with Overwrite
 * strategy</li>
 * <li><b>Post-step</b> — refreshes all materialized views</li>
 * </ol>
 *
 * <p>
 * Transaction boundary: one database transaction per chunk (chunk-size = 250).
 * A chunk failure rolls back only that chunk (overview.md §14.3).
 *
 * <p>
 * Concurrency: only one job may run at a time; enforced by
 * {@code ImportService}.
 */
@Configuration
@Profile("!test")
@RequiredArgsConstructor
public class UniProtImportJobConfig {

    private final AppProperties appProperties;

    /**
     * Dynamic reader factory. StepScope allows accessing jobParameters.
     */
    @Bean
    @StepScope
    DelegatingItemStreamReader<String> dynamicUniprotReader(UniProtImportJobParameters params) {
        var filePath = params.getFilePath();
        var resource = new FileSystemResource(filePath);
        ItemStreamReader<String> delegate;
        if (filePath.toLowerCase().endsWith(".dat")) {
            delegate = new UniprotDatItemReader(resource);
        } else if (filePath.toLowerCase().endsWith(".tsv")) {
            delegate = new FlatFileItemReaderBuilder<String>()
                    .name("tsvReader")
                    .resource(resource)
                    .lineMapper((line, _) -> line)
                    .linesToSkip(1) // skip header
                    .build();
        } else {
            throw new IllegalArgumentException("Unsupported file extension");
        }
        return new DelegatingItemStreamReader<>(delegate);
    }

    @Bean
    Step uniProtImportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemStreamReader<String> dynamicUniprotReader,
            ProteinEntryItemProcessor processor,
            ProteinAggregateItemWriter writer,
            ImportProgressChunkListener progressChunkListener) {

        return new StepBuilder(Constants.IMPORT_STEP.getKey(), jobRepository)
                .<String, ProteinEntry>chunk(appProperties.getBatch().getChunkSize())
                .listener(progressChunkListener)
                .transactionManager(transactionManager)
                .reader(dynamicUniprotReader)
                .processor(processor)
                .writer(writer)
                .faultTolerant() // Allows configuring skip policies
                .skip(MalformedUniprotFileException.class) // skip malformed uniprot
                .skip(ConstraintViolationException.class) // skip SQL constraint violation
                .skip(StaleObjectStateException.class) // skip concurrency access
                .skipLimit(appProperties.getBatch().getSkipLimit())
                .listener(new ImportUniprotSkipListener())
                .build();
    }

    @Bean
    Job uniProtImportJob(JobRepository jobRepository, Step uniProtImportStep,
                         ImportJobDatabaseListener databaseListener,
                         PostImportCacheEvictionListener postImportCacheEvictionListener,
                         ImportJobRefreshViewsListener refreshViewsListener) {
        return new JobBuilder(Constants.IMPORT_FILE_JOB.getKey(), jobRepository)
                .start(uniProtImportStep)
                .listener(databaseListener)
                .listener(postImportCacheEvictionListener)
                .listener(refreshViewsListener)
                .build();
    }

}
