package com.bioinformatics.dashboard.gene.dto;

public record ProteinFeatureDto(long id, String featureType, int startPos, int endPos, String note, String featureId) {
}
