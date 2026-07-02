package com.bioinformatics.dashboard.model.gene;

public record CrossReferenceDto(long id, String source, String identifier, String secondaryId, String tertiaryInfo) {
}
