package com.bioinformatics.dashboard.model.gene;

/**
 * External database cross-reference record used in protein detail responses.
 * Links proteins to identifiers in other databases (e.g., UniProt, InterPro, PDB).
 */
public record CrossReferenceDto(long id, String source, String identifier, String secondaryId, String tertiaryInfo) {
}
