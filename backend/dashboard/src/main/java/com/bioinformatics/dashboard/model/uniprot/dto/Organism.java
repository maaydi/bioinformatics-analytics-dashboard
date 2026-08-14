package com.bioinformatics.dashboard.model.uniprot.dto;

import java.util.List;

public record Organism(
        String scientificName,
        String commonName,
        int taxonId,
        List<String> lineage
) {
}
