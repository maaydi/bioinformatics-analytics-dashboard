package com.bioinformatics.analyticsservice.models;

/**
 * Indicates frequency of manual validation status flags within a dataset.
 */
public record ReviewedRatioDto(boolean reviewed, long count) {
}
