package com.bioinformatics.exportservice.processor;

import com.bioinformatics.common.gene.dto.ProteinDetailDto;
import com.bioinformatics.common.gene.entity.ProteinEntry;
import com.bioinformatics.common.gene.mapper.GeneMapper;
import com.bioinformatics.common.uniprot.dto.UniProtEntry;
import com.bioinformatics.common.uniprot.mapper.UniProtEntryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Spring Batch {@link ItemProcessor} that converts a {@link UniProtEntry} (fetched from the API)
 * into a {@link ProteinDetailDto} for batch export.
 *
 * <h3>Processing pipeline</h3>
 * <ol>
 *   <li><b>Entity mapping</b> — {@link UniProtEntryMapper} converts the REST DTO into a
 *       {@link ProteinEntry}.</li>
 *   <li><b>DTO mapping</b> — {@link GeneMapper} transforms the entity into a {@link ProteinDetailDto}
 *       for export.</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UniProtApiEntryProcessor implements ItemProcessor<UniProtEntry, ProteinDetailDto> {

    private final UniProtEntryMapper mapper;
    private final GeneMapper geneMapper;

    /**
     * Processes a single {@link UniProtEntry}.
     *
     * @param item the entry to process; never {@code null}
     * @return the mapped {@link ProteinDetailDto}, or {@code null} to skip duplicates
     */
    @Override
    public @Nullable ProteinDetailDto process(@NonNull UniProtEntry item) {
        var entry = mapper.toProteinEntry(item);

        return geneMapper.toDetail(entry);
    }
}

