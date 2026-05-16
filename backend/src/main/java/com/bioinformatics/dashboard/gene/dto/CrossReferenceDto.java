package com.bioinformatics.dashboard.gene.dto;

public record CrossReferenceDto(long id, String source, String identifier, String secondaryId, String tertiaryInfo) {
}
