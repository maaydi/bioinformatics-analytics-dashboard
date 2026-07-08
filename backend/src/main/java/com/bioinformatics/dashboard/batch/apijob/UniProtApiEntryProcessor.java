package com.bioinformatics.dashboard.batch.apijob;

import com.bioinformatics.dashboard.batch.processor.resolver.GoTermResolver;
import com.bioinformatics.dashboard.batch.processor.resolver.KeywordResolver;
import com.bioinformatics.dashboard.batch.processor.resolver.ProteinAccessionResolver;
import com.bioinformatics.dashboard.providers.postgres.gene.entity.ProteinEntry;
import com.bioinformatics.dashboard.providers.uniprotkb.dto.UniProtEntry;
import com.bioinformatics.dashboard.providers.uniprotkb.mapper.UniProtEntryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Spring Batch {@link ItemProcessor} that converts a {@link UniProtEntry} (fetched from the API)
 * into a fully populated {@link ProteinEntry} ready for persistence.
 *
 * <h3>Processing pipeline</h3>
 * <ol>
 *   <li><b>Duplicate check</b> — if the accession already exists in the database (via
 *       {@link ProteinAccessionResolver}), the entry is skipped by returning {@code null}.</li>
 *   <li><b>Mapping</b> — {@link UniProtEntryMapper} converts the REST DTO into a JPA aggregate
 *       (entity + transient child collections).</li>
 *   <li><b>Keyword resolution</b> — transient {@link com.bioinformatics.dashboard.providers.postgres.gene.entity.Keyword}
 *       objects produced by the mapper are resolved against the database via {@link KeywordResolver},
 *       which performs a find-or-create strategy with an in-memory cache.</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UniProtApiEntryProcessor implements ItemProcessor<UniProtEntry, ProteinEntry> {

    private final UniProtEntryMapper mapper;
    private final KeywordResolver keywordResolver;
    private final ProteinAccessionResolver accessionResolver;
    private final GoTermResolver termResolver;

    /**
     * Processes a single {@link UniProtEntry}.
     *
     * @param item the entry to process; never {@code null}
     * @return the mapped {@link ProteinEntry}, or {@code null} to skip duplicates
     */
    @Override
    public @Nullable ProteinEntry process(@NonNull UniProtEntry item) {
        var accession = item.primaryAccession();

        if (accession == null || accession.isBlank()) {
            log.warn("Skipping UniProtEntry with blank accession");
            return null;
        }

        if (accessionResolver.alreadyExists(accession)) {
            log.debug("Skipping duplicate accession: {}", accession);
            return null;
        }

        var entry = mapper.toProteinEntry(item);

        // Replace transient keyword stubs with managed/persisted entities
        var resolvedKeywords = keywordResolver.resolveKeywords(entry.getKeywords());
        entry.setKeywords(resolvedKeywords);

        // Replace transient GoTerm stubs with managed/persisted entities
        var resolvedGoTerms = termResolver.resolveGoTerms(entry.getGoTerms());
        entry.setGoTerms(resolvedGoTerms);

        return entry;
    }
}

