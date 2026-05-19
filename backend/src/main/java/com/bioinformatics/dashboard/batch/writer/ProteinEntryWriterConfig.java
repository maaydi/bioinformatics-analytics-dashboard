package com.bioinformatics.dashboard.batch.writer;

import com.bioinformatics.dashboard.gene.repository.CrossReferenceRepository;
import com.bioinformatics.dashboard.gene.repository.ProteinCommentRepository;
import com.bioinformatics.dashboard.gene.repository.ProteinEntryRepository;
import com.bioinformatics.dashboard.gene.repository.ProteinPublicationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProteinEntryWriterConfig {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Aggregate ItemWriter that persists ProteinEntry and explicitly saves children
     * (cross-references, comments, publications) via their own repositories after
     * the parent is flushed — avoiding CascadeType.ALL overhead on large collections.
     */
    @Bean
    public ProteinAggregateItemWriter proteinAggregateItemWriter(
            ProteinEntryRepository proteinEntryRepository,
            CrossReferenceRepository crossReferenceRepository,
            ProteinCommentRepository proteinCommentRepository,
            ProteinPublicationRepository proteinPublicationRepository) {

        return new ProteinAggregateItemWriter(
                proteinEntryRepository,
                crossReferenceRepository,
                proteinCommentRepository,
                proteinPublicationRepository,
                entityManager);
    }
}