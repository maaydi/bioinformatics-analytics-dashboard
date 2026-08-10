package com.bioinformatics.dashboard.model.gene;

import lombok.Builder;

/**
 * Gene Ontology (GO) term record used in protein detail responses.
 * Represents a GO classification (Process, Function, or Component) assigned to a protein.
 */
@Builder
public record GoTermDto(int id, String goId, Character aspect, String description) {
}
