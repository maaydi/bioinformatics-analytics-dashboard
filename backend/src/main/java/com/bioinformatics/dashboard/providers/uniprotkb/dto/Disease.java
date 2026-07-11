package com.bioinformatics.dashboard.providers.uniprotkb.dto;

import java.util.List;

public record Disease(
        String diseaseId,
        String diseaseAccession,
        String acronym,
        String description,
        DiseaseCrossReference diseaseCrossReference,
        List<Evidence> evidences
) {
}
