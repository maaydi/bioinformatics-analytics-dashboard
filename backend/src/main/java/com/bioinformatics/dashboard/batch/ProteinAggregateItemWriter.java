package com.bioinformatics.dashboard.batch;

import com.bioinformatics.dashboard.gene.entity.CrossReference;
import com.bioinformatics.dashboard.gene.entity.ProteinComment;
import com.bioinformatics.dashboard.gene.entity.ProteinEntry;
import com.bioinformatics.dashboard.gene.entity.ProteinPublication;
import com.bioinformatics.dashboard.gene.repository.CrossReferenceRepository;
import com.bioinformatics.dashboard.gene.repository.ProteinCommentRepository;
import com.bioinformatics.dashboard.gene.repository.ProteinEntryRepository;
import com.bioinformatics.dashboard.gene.repository.ProteinPublicationRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Batch ItemWriter that persists a {@link ProteinEntry} and its children
 * (cross-references, comments, publications) in the correct order to avoid
 * issues with CascadeType.ALL on large collections.
 *
 * <p>ProteinEntry (with features and hostOrganisms cascaded through JPA) is
 * persisted first. Then cross-references, comments, and publications are saved
 * explicitly via their own repositories so that Hibernate can batch-insert them
 * without holding the entire object graph in memory.
 */
@Slf4j
@RequiredArgsConstructor
public class ProteinAggregateItemWriter implements ItemWriter<ProteinEntry> {

    private final ProteinEntryRepository proteinEntryRepository;
    private final CrossReferenceRepository crossReferenceRepository;
    private final ProteinCommentRepository proteinCommentRepository;
    private final ProteinPublicationRepository proteinPublicationRepository;
    private final EntityManager entityManager;

    @Override
    public void write(Chunk<? extends ProteinEntry> chunk) throws Exception {
        var items = chunk.getItems();

        // Collect children before saving parents (ids not yet assigned)
        List<CrossReference> allCrossRefs = new ArrayList<>();
        List<ProteinComment> allComments = new ArrayList<>();
        List<ProteinPublication> allPublications = new ArrayList<>();

        for (ProteinEntry entry : items) {
            allCrossRefs.addAll(entry.getCrossReferences());
            allComments.addAll(entry.getComments());
            allPublications.addAll(entry.getPublications());
        }

        // 1. Persist ProteinEntry (cascades features + hostOrganisms)
        proteinEntryRepository.saveAll(items);
        entityManager.flush();

        // 2. Ensure children have the protein reference set (protein.id is now populated)
        for (ProteinEntry entry : items) {
            entry.getCrossReferences().forEach(c -> c.setProtein(entry));
            entry.getComments().forEach(c -> c.setProtein(entry));
            entry.getPublications().forEach(p -> p.setProtein(entry));
        }

        // 3. Persist children explicitly
        if (!allCrossRefs.isEmpty()) {
            crossReferenceRepository.saveAll(allCrossRefs);
        }
        if (!allComments.isEmpty()) {
            proteinCommentRepository.saveAll(allComments);
        }
        if (!allPublications.isEmpty()) {
            proteinPublicationRepository.saveAll(allPublications);
        }

        entityManager.flush();
        entityManager.clear();

        log.debug("Wrote chunk: {} protein entries, {} cross-refs, {} comments, {} publications",
                items.size(), allCrossRefs.size(), allComments.size(), allPublications.size());
    }
}
