package com.bioinformatics.dashboard.model.uniprot.dto;

import java.util.List;

public record UniProtKBCrossReference(
        String database,
        String id,
        List<CrossReferenceProperty> properties,
        List<Evidence> evidences
) {
}
