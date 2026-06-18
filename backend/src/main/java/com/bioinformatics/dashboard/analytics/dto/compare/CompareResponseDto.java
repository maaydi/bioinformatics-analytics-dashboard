package com.bioinformatics.dashboard.analytics.dto.compare;

/**
 * DTO containing analytics results for two compared filter sets.
 *
 * @param subsetA analytics data for first filter set
 * @param subsetB analytics data for second filter set
 */
public record CompareResponseDto(AnalyticsSubsetDto subsetA, AnalyticsSubsetDto subsetB) {
}
