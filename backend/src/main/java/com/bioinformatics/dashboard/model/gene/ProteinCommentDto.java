package com.bioinformatics.dashboard.model.gene;

/**
 * Protein comment record containing annotations and notes about the protein.
 * Examples: catalytic activity, function, subcellular location, disease association.
 */
public record ProteinCommentDto(long id, String commentType, String text) {
}
