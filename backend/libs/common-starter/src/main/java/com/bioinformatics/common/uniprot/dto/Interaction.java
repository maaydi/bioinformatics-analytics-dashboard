package com.bioinformatics.common.uniprot.dto;

public record Interaction(
        Interactant interactantOne,
        Interactant interactantTwo,
        int numberOfExperiments,
        boolean organismDiffer
) {
}
