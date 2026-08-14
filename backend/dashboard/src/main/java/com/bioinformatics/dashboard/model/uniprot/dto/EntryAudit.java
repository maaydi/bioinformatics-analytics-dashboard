package com.bioinformatics.dashboard.model.uniprot.dto;

public record EntryAudit(
        String firstPublicDate,
        String lastAnnotationUpdateDate,
        String lastSequenceUpdateDate,
        int entryVersion,
        int sequenceVersion
) {
}

