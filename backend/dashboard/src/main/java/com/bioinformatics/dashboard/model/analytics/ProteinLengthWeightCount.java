package com.bioinformatics.dashboard.model.analytics;

/**
 * Captures raw geometric combinations of sequence length and molecular mass without bucketing constraints.
 */
public record ProteinLengthWeightCount(int length, int moleculeWeight, long count) {
}
