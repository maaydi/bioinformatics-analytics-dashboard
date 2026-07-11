package com.bioinformatics.dashboard.providers.uniprotkb.dto;

import java.util.List;

public record UniProtKBCrossReference(
        String database,
        String id,
        List<CrossReferenceProperty> properties,
        List<Evidence> evidences
) {
}
