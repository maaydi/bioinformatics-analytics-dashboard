package com.bioinformatics.dashboard.providers.uniprotkb.dto;

public record EntryAudit(
        String firstPublicDate,
        String lastAnnotationUpdateDate,
        String lastSequenceUpdateDate,
        int entryVersion,
        int sequenceVersion
) {
}

