package com.bioinformatics.dashboard.analytics.dto;

/**
 * Indicates frequency of manual validation status flags within a dataset.
 */
public record ReviewedRatioDto(boolean reviewed, long count) {
}
