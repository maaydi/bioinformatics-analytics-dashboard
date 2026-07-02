package com.bioinformatics.dashboard.model.gene;

public record ProteinFeatureDto(long id, String featureType, int startPos, int endPos, String note, String featureId,
                                String evidence) {
}
