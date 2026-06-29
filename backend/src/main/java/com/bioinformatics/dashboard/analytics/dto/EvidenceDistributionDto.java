package com.bioinformatics.dashboard.analytics.dto;

/**
 * Formats evidence levels for visual distribution breakdown.
 */
public record EvidenceDistributionDto(int evidenceLevel, String label, long count) {

}
