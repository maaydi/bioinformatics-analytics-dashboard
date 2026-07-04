package com.bioinformatics.dashboard.providers.uniprotkb.dto;

public record Interaction(
        Interactant interactantOne,
        Interactant interactantTwo,
        int numberOfExperiments,
        boolean organismDiffer
) {
}
