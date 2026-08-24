package com.bioinformatics.analyticsservice.models.compare;


import com.bioinformatics.analyticsservice.models.EvidenceDistributionDto;
import com.bioinformatics.analyticsservice.models.LengthHistogramBucketDto;

import java.util.List;

/**
 * DTO containing aggregated analytics metrics for a filtered protein subset.
 *
 * @param count                total number of proteins
 * @param avgLength            average protein length in amino acids
 * @param reviewedCount        number of reviewed proteins
 * @param reviewedRatio        percentage of reviewed proteins
 * @param lengthDistribution   histogram buckets for protein length distribution
 * @param evidenceDistribution distribution by evidence level
 */
public record AnalyticsSubsetDto(long count,
                                 long avgLength,
                                 long reviewedCount,
                                 long reviewedRatio,
                                 List<LengthHistogramBucketDto> lengthDistribution,
                                 List<EvidenceDistributionDto> evidenceDistribution) {
}
