package com.bioinformatics.dashboard.providers.uniprotkb.dto;

import java.util.List;

public record Organism(
        String scientificName,
        String commonName,
        int taxonId,
        List<String> lineage
) {
}
