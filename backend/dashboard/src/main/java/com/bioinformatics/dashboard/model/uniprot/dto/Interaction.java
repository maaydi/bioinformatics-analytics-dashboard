package com.bioinformatics.dashboard.model.uniprot.dto;

public record Interaction(
        Interactant interactantOne,
        Interactant interactantTwo,
        int numberOfExperiments,
        boolean organismDiffer
) {
}
