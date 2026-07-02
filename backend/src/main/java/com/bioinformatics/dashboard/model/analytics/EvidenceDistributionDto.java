package com.bioinformatics.dashboard.model.analytics;

/**
 * Formats evidence levels for visual distribution breakdown.
 */
public record EvidenceDistributionDto(int evidenceLevel, String label, long count) {

}
