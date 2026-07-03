package com.bioinformatics.dashboard.model.gene;

/**
 * Protein feature record indicating a functionally significant region or domain within the sequence.
 * Examples: transmembrane domain, signal peptide, active site, zinc finger.
 */
public record ProteinFeatureDto(long id, String featureType, int startPos, int endPos, String note, String featureId,
                                String evidence) {
}
