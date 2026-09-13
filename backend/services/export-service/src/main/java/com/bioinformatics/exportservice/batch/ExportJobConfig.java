package com.bioinformatics.exportservice.batch;

import com.bioinformatics.common.batch.DelegatingItemStreamReader;
import com.bioinformatics.common.gene.entity.ProteinEntry;
import com.bioinformatics.common.uniprot.dto.UniProtEntry;
import com.bioinformatics.exportservice.config.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
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

@Configuration
@Profile("!test")
@RequiredArgsConstructor
public class ExportJobConfig {

    private final ApplicationProperties appProperties;

    /**
     * Dynamic reader factory. StepScope allows accessing jobParameters.
     */
    @Bean
    @StepScope
    DelegatingItemStreamReader<String> dynamicUniprotReader(ExportJobParameters params) {
        // TODO based on provider
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
    Step exportChunkStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemStreamReader<UniProtEntry> uniProtApiItemReader,
            UniProtApiEntryProcessor processor,
            ProteinAggregateItemWriter writer, ImportProgressChunkListener progressChunkListener) {

        return new StepBuilder(Constants.API_IMPORT_STEP.getKey(), jobRepository)
                .<UniProtEntry, ProteinEntry>chunk(appProperties.batch().chunkSize())
                .listener(progressChunkListener)
                .transactionManager(transactionManager)
                .reader(uniProtApiItemReader)
                .processor(processor)
                .writer(writer)
                .build();
    }


    @Bean
    Job exportPipelineJob(JobRepository jobRepository,
                          Step uniProtApiImportStep) {

    }
}
