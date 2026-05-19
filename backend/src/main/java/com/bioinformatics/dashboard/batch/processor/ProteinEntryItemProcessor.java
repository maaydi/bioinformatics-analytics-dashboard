package com.bioinformatics.dashboard.batch.processor;

import com.bioinformatics.dashboard.batch.processor.resolver.KeywordResolver;
import com.bioinformatics.dashboard.gene.entity.ProteinEntry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class ProteinEntryItemProcessor implements ItemProcessor<String, ProteinEntry> {

    /**
     * Transforms raw UniProt record text into a {@link ProteinEntry} entity.
     *
     * <p>Performs parsing, basic validation and resolves related entities
     * (keywords, cross-references, publications, features) before returning
     * a ready-to-persist entity. Returns null to skip records (e.g. duplicates).
     */

    private final KeywordResolver keywordResolver;
    private final LineProcessorRegistry registry;

    @Override
    public @Nullable ProteinEntry process(@NonNull String item) {
        if (item.trim().isEmpty())
            return null;
        var context = new ProteinParsingContext();
        var lines = item.split("\n");
        for (var line : lines) {
            registry.process(line, context);
            if (context.isSkipEntry()) {
                log.warn("Duplicated accession. Skipping Protein entry.");
                return null;
            }
        }
        // Resolve keywords using the injected bean, keeping context strictly data
        var resolvedKeywords = keywordResolver.resolveKeywords(context.getKeywords());
        return context.build(resolvedKeywords);


    }


}