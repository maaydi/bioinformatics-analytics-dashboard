package com.bioinformatics.dashboard.interfaces.analytics;

import com.bioinformatics.dashboard.interfaces.Provider;
import com.bioinformatics.dashboard.model.analytics.*;

import java.util.List;

public interface AnalyticsService extends Provider {
    DashboardKpisDto getDashboardKpis();

    List<LengthHistogramBucketDto> getLengthHistogram();

    List<OrganismCountDto> getByOrganism(int limit);

    List<ReviewedRatioDto> getReviewedRatio();

    List<EvidenceDistributionDto> getEvidenceLevels();

    List<KeywordFrequencyDto> getKeywordFrequency(int limit);
}
