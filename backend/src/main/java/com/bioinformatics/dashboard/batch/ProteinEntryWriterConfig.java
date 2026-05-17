package com.bioinformatics.dashboard.batch;

import com.bioinformatics.dashboard.gene.entity.ProteinEntry;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.infrastructure.item.database.JpaItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JpaItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProteinEntryWriterConfig {

    /**
     * JPA-backed ItemWriter for persisting ProteinEntry entities within a batch chunk.
     * Uses the provided EntityManagerFactory.
     */
    @Bean
    public JpaItemWriter<ProteinEntry> proteinEntryItemWriter(EntityManagerFactory entityManagerFactory) {

        return new JpaItemWriterBuilder<ProteinEntry>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}