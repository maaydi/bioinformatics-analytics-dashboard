package com.bioinformatics.dashboard.model.gene;

import lombok.Builder;

/**
 * Protein feature record indicating a functionally significant region or domain within the sequence.
 * Examples: transmembrane domain, signal peptide, active site, zinc finger.
 */
@Builder
public record ProteinFeatureDto(long id, String featureType, int startPos, int endPos, String note, String featureId,
                                String evidence) {
}
