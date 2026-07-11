package com.bioinformatics.dashboard.providers.uniprotkb.dto;

import java.util.List;

public record UniProtEntry(
        String entryType,
        String primaryAccession,
        String uniProtkbId,
        EntryAudit entryAudit,
        int annotationScore,
        Organism organism,
        String proteinExistence,
        ProteinDescription proteinDescription,
        List<Gene> genes,
        List<Comment> comments,
        List<Feature> features,
        List<Keyword> keywords,
        List<Reference> references,
        List<UniProtKBCrossReference> uniProtKBCrossReferences,
        Sequence sequence,
        ExtraAttributes extraAttributes
) {
}
