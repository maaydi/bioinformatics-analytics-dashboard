package com.bioinformatics.dashboard.providers.uniprotkb.dto;

import java.util.List;

public record Feature(
        String type,
        FeatureLocation location,
        String description,
        String featureId,
        List<Evidence> evidences,
        List<FeatureCrossReference> featureCrossReferences,
        AlternativeSequence alternativeSequence
) {
}
