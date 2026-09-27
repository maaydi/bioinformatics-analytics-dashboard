package com.bioinformatics.exportservice.batch;

import com.bioinformatics.common.batch.reader.UniProtApiItemReader;
import com.bioinformatics.common.batch.reader.UniProtPostgresItemReader;
import com.bioinformatics.common.config.CommonProperties;
import com.bioinformatics.common.exception.ResourceNotFoundException;
import com.bioinformatics.common.gene.entity.ProteinEntry;
import com.bioinformatics.common.gene.service.ProteinEntryService;
import com.bioinformatics.common.providers.uniprotkb.service.UniProtApiClient;
import com.bioinformatics.common.uniprot.dto.UniProtEntry;
import com.bioinformatics.exportservice.config.ApplicationProperties;
import com.bioinformatics.exportservice.dto.Constants;
import com.bioinformatics.exportservice.dto.converter.ExportFormatRegistry;
import com.bioinformatics.exportservice.listener.ExportProgressChunkListener;
import com.bioinformatics.exportservice.processor.ProteinDetailProcessor;
import com.bioinformatics.exportservice.processor.UniProtApiEntryProcessor;
import com.bioinformatics.exportservice.processor.UniProtPostgresEntryProcessor;
import com.bioinformatics.exportservice.repository.ExportJobExecutionRepository;
import com.bioinformatics.exportservice.service.ExportFileStorageService;
import com.bioinformatics.exportservice.writer.ExportItemWriter;
import com.bioinformatics.exportservice.writer.ExportWriterFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.support.CompositeItemProcessor;
import org.springframework.batch.infrastructure.item.support.builder.CompositeItemProcessorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

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
 *   <li><b>Writer</b> ({@link ExportItemWriter}) — persists the aggregate in the
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
public class UniProtExportJobStepConfig {

    private final ApplicationProperties appProperties;
    private final CommonProperties commonProperties;
    private final ExportJobExecutionRepository jobExecutionRepository;
    private final ProteinEntryService proteinEntryService;


    /**
     * Step-scoped reader so that a fresh reader instance (and an empty buffer) is
     * created for every step execution — including restarts.
     */
    @Bean
    @StepScope
    UniProtApiItemReader uniProtApiItemReader(UniProtApiClient apiClient, ExportJobParameters params) {
        var filter = jobExecutionRepository.findByPipelineId(params.getJobId());
        if (filter.isEmpty()) {
            throw new ResourceNotFoundException("No Filter found for export pipeline %d".formatted(params.getJobId()));
        }
        var request = filter.get().getPipeline().getFilterJson().copy()
                .page(0)
                .size(commonProperties.uniprotApi().batch().chunkSize())
                .build();
        return new UniProtApiItemReader(apiClient, request);
    }

    @Bean
    @StepScope
    public CompositeItemProcessor<UniProtEntry, Map<String, Object>> compositeUniProtApiProcessor(
            UniProtApiEntryProcessor uniProtApiEntryProcessor,
            ProteinDetailProcessor proteinDetailProcessor) {

        return new CompositeItemProcessorBuilder<UniProtEntry, Map<String, Object>>()
                .delegates(uniProtApiEntryProcessor, proteinDetailProcessor)
                .build();
    }

    @Bean
    @StepScope
    public ExportItemWriter proteinItemWriter(
            ExportJobParameters params,
            ExportWriterFactory writerFactory,
            ExportFileStorageService storageService) {
        var format = ExportFormatRegistry.getFormat(params.getExportFormat());
        var formatWriter = writerFactory.getWriter(format);

        return new ExportItemWriter(
                formatWriter,
                storageService,
                params.getInitiatorUserId(),
                params.getJobId(),
                format,
                params.getExportedFields()
        );
    }

    @Bean
    Step uniProtApiExportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemStreamReader<UniProtEntry> uniProtApiItemReader,
            ItemProcessor<UniProtEntry, Map<String, Object>> compositeUniProtApiProcessor,
            ExportItemWriter writer,
            ExportProgressChunkListener progressChunkListener) {

        return new StepBuilder(Constants.API_EXPORT_STEP.getKey(), jobRepository)
                .<UniProtEntry, Map<String, Object>>chunk(commonProperties.uniprotApi().batch().chunkSize())
                .listener(progressChunkListener)
                .transactionManager(transactionManager)
                .reader(uniProtApiItemReader)
                .processor(compositeUniProtApiProcessor)
                .writer(writer)
                .build();
    }

    /**
     * Step-scoped reader so that a fresh reader instance (and an empty buffer) is
     * created for every step execution — including restarts.
     */
    @Bean
    @StepScope
    UniProtPostgresItemReader uniProtPostgresItemReader(ExportJobParameters params) {
        var filter = jobExecutionRepository.findByPipelineId(params.getJobId());
        if (filter.isEmpty()) {
            throw new ResourceNotFoundException("No Filter found for export pipeline %d".formatted(params.getJobId()));
        }
        var chunkSize = appProperties.export().batch().chunkSize();
        var request = filter.get().getPipeline().getFilterJson().copy()
                .page(0)
                .build();
        return new UniProtPostgresItemReader(proteinEntryService, request, chunkSize);
    }


    @Bean
    @StepScope
    public CompositeItemProcessor<ProteinEntry, Map<String, Object>> compositeUniProtPostgresEntryProcessor(
            UniProtPostgresEntryProcessor uniProtPostgresEntryProcessor,
            ProteinDetailProcessor proteinDetailProcessor) {

        return new CompositeItemProcessorBuilder<ProteinEntry, Map<String, Object>>()
                .delegates(uniProtPostgresEntryProcessor, proteinDetailProcessor)
                .build();
    }


    @Bean
    Step uniProtPostgresExportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemStreamReader<ProteinEntry> uniProtPostgresItemReader,
            ItemProcessor<ProteinEntry, Map<String, Object>> compositeUniProtPostgresEntryProcessor,
            ExportItemWriter writer,
            ExportProgressChunkListener progressChunkListener) {

        return new StepBuilder(Constants.POSTGRES_EXPORT_STEP.getKey(), jobRepository)
                .<ProteinEntry, Map<String, Object>>chunk(appProperties.export().batch().chunkSize())
                .listener(progressChunkListener)
                .transactionManager(transactionManager)
                .reader(uniProtPostgresItemReader)
                .processor(compositeUniProtPostgresEntryProcessor)
                .writer(writer)
                .build();
    }
}

