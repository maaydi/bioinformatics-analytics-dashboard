package com.bioinformatics.dashboard.job.uniprot.apiloader;

import com.bioinformatics.dashboard.config.AppProperties;
import com.bioinformatics.dashboard.exception.ResourceNotFoundException;
import com.bioinformatics.dashboard.interfaces.UniProtApiClient;
import com.bioinformatics.dashboard.job.dto.Constants;
import com.bioinformatics.dashboard.job.listener.ImportJobDatabaseListener;
import com.bioinformatics.dashboard.job.listener.ImportJobRefreshViewsListener;
import com.bioinformatics.dashboard.job.listener.ImportProgressChunkListener;
import com.bioinformatics.dashboard.job.listener.PostImportCacheEvictionListener;
import com.bioinformatics.dashboard.job.uniprot.apiloader.processor.UniProtApiEntryProcessor;
import com.bioinformatics.dashboard.job.uniprot.apiloader.reader.UniProtApiItemReader;
import com.bioinformatics.dashboard.job.writer.ProteinAggregateItemWriter;
import com.bioinformatics.dashboard.model.uniprot.dto.UniProtEntry;
import com.bioinformatics.dashboard.providers.postgres.gene.entity.ProteinEntry;
import com.bioinformatics.dashboard.savedfilter.service.SavedFilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch configuration for the UniProt <em>API-based</em> import pipeline.
 *
 * <p>This job fetches protein data directly from the UniProt REST API using
 * sequential pagination, in contrast to the file-based {@code UniProtImportJobConfig}
 * which reads from an uploaded {@code .dat} or {@code .tsv} file.
 *
 * <h3>Pipeline</h3>
 * <ol>
 *   <li><b>Reader</b> ({@link UniProtApiItemReader}) — pages through the API sequentially,
 *       buffering entries and exposing them one at a time to the step.</li>
 *   <li><b>Processor</b> ({@link UniProtApiEntryProcessor}) — deduplicates, maps the
 *       REST DTO to a JPA aggregate, and resolves keyword entities.</li>
 *   <li><b>Writer</b> ({@link ProteinAggregateItemWriter}) — persists the aggregate in the
 *       correct order: {@code ProteinEntry} first (with cascaded {@code features} and
 *       {@code hostOrganisms}), then cross-references, comments, and publications.</li>
 * </ol>
 *
 * <h3>Chunk size</h3>
 * Controlled by {@code app.batch.chunk-size}. Each chunk is a single database transaction.
 *
 * <h3>API page size</h3>
 * Controlled by {@code app.batch.api-page-size} (default 500). This is independent of the
 * chunk size: the reader accumulates API results in a buffer that the step drains in chunks.
 *
 * <h3>Restartability</h3>
 * The reader persists its current page index to the Spring Batch {@link org.springframework.batch.infrastructure.item.ExecutionContext},
 * so the job can resume from the last committed chunk after a failure.
 */
@Configuration
@Profile("!test")
@RequiredArgsConstructor
public class UniProtApiImportJobConfig {

    private final AppProperties appProperties;
    private final SavedFilterService savedFilterService;


    /**
     * Step-scoped reader so that a fresh reader instance (and an empty buffer) is
     * created for every step execution — including restarts.
     */
    @Bean
    @StepScope
    UniProtApiItemReader uniProtApiItemReader(UniProtApiClient apiClient, UniProtApiImportJobParameters params) {
        var filter = savedFilterService.getSavedFilterById(params.getFilterId());
        if (filter.isEmpty()) {
            throw new ResourceNotFoundException("Filter with id %d not found".formatted(params.getFilterId()));
        }
        var request = filter.get().filterJson().copy().page(0).size(appProperties.getUniprotApi().getBatch().getChunkSize()).build();
        return new UniProtApiItemReader(apiClient, request);
    }


    @Bean
    Step uniProtApiImportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemStreamReader<UniProtEntry> uniProtApiItemReader,
            UniProtApiEntryProcessor processor,
            ProteinAggregateItemWriter writer, ImportProgressChunkListener progressChunkListener) {

        return new StepBuilder(Constants.API_IMPORT_STEP.getKey(), jobRepository)
                .<UniProtEntry, ProteinEntry>chunk(appProperties.getBatch().getChunkSize())
                .listener(progressChunkListener)
                .transactionManager(transactionManager)
                .reader(uniProtApiItemReader)
                .processor(processor)
                .writer(writer)
                .build();
    }


    @Bean
    Job uniProtApiImportJob(
            JobRepository jobRepository,
            Step uniProtApiImportStep,
            ImportJobDatabaseListener databaseListener,
            PostImportCacheEvictionListener cacheEvictionListener,
            ImportJobRefreshViewsListener refreshViewsListener) {

        return new JobBuilder(Constants.IMPORT_API_JOB.getKey(), jobRepository)
                .start(uniProtApiImportStep)
                .listener(databaseListener)
                .listener(cacheEvictionListener)
                .listener(refreshViewsListener)
                .build();
    }
}

