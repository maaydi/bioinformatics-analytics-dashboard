package com.bioinformatics.dashboard.analytics.dto.compare;

import com.bioinformatics.dashboard.analytics.dto.EvidenceDistributionDto;
import com.bioinformatics.dashboard.analytics.dto.LengthHistogramBucketDto;

import java.util.List;

public record AnalyticsSubsetDto(long count, long avgLength, long reviewedCount, long reviewedRatio,
                                 List<LengthHistogramBucketDto> lengthDistribution,
                                 List<EvidenceDistributionDto> evidenceDistribution) {
}
