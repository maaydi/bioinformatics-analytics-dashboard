package com.bioinformatics.dashboard.model.gene;

import lombok.Builder;

/**
 * Protein comment record containing annotations and notes about the protein.
 * Examples: catalytic activity, function, subcellular location, disease association.
 */
@Builder
public record ProteinCommentDto(long id, String commentType, String text) {
}
