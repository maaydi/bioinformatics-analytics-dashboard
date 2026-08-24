package com.bioinformatics.dashboard.batch.writer;

import com.bioinformatics.common.gene.entity.CrossReference;
import com.bioinformatics.common.gene.entity.ProteinComment;
import com.bioinformatics.common.gene.entity.ProteinEntry;
import com.bioinformatics.common.gene.entity.ProteinPublication;
import com.bioinformatics.dashboard.job.writer.ProteinAggregateItemWriter;
import com.bioinformatics.dashboard.providers.postgres.gene.repository.CrossReferenceRepository;
import com.bioinformatics.dashboard.providers.postgres.gene.repository.ProteinCommentRepository;
import com.bioinformatics.dashboard.providers.postgres.gene.repository.ProteinEntryRepository;
import com.bioinformatics.dashboard.providers.postgres.gene.repository.ProteinPublicationRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.Chunk;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProteinAggregateItemWriterTest {

    @Mock
    private ProteinEntryRepository proteinEntryRepository;
    @Mock
    private CrossReferenceRepository crossReferenceRepository;
    @Mock
    private ProteinCommentRepository proteinCommentRepository;
    @Mock
    private ProteinPublicationRepository proteinPublicationRepository;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ProteinAggregateItemWriter writer;

    @Test
    void shouldSaveProteinAndChildren() throws Exception {
        // Arrange
        ProteinEntry entry1 = new ProteinEntry();
        entry1.setAccession("P12345");

        CrossReference ref1 = new CrossReference();
        entry1.getCrossReferences().add(ref1);

        ProteinComment comment1 = new ProteinComment();
        entry1.getComments().add(comment1);

        ProteinPublication pub1 = new ProteinPublication();
        entry1.getPublications().add(pub1);

        Chunk<ProteinEntry> chunk = new Chunk<>(List.of(entry1));

        // Act
        writer.write(chunk);

        // Assert
        verify(proteinEntryRepository).saveAll(chunk.getItems());
        verify(crossReferenceRepository).saveAll(anyIterable());
        verify(proteinCommentRepository).saveAll(anyIterable());
        verify(proteinPublicationRepository).saveAll(anyIterable());
        verify(entityManager, times(2)).flush();
        verify(entityManager).clear();
    }

    @Test
    void shouldHandleEmptyChildren() throws Exception {
        // Arrange
        ProteinEntry entry1 = new ProteinEntry();
        entry1.setAccession("P12345");

        Chunk<ProteinEntry> chunk = new Chunk<>(List.of(entry1));

        // Act
        writer.write(chunk);

        // Assert
        verify(proteinEntryRepository).saveAll(chunk.getItems());
        verify(crossReferenceRepository, never()).saveAll(anyIterable());
        verify(proteinCommentRepository, never()).saveAll(anyIterable());
        verify(proteinPublicationRepository, never()).saveAll(anyIterable());
        verify(entityManager, times(2)).flush();
        verify(entityManager).clear();
    }
}

