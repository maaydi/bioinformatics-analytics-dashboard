package com.bioinformatics.analyticsservice.models;

/**
 * Formats evidence levels for visual distribution breakdown.
 */
public record EvidenceDistributionDto(int evidenceLevel, String label, long count) {

}
