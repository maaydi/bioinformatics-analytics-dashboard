package com.bioinformatics.dashboard.interfaces.analytics;

import com.bioinformatics.dashboard.interfaces.Provider;
import com.bioinformatics.dashboard.model.analytics.*;
import com.bioinformatics.dashboard.model.analytics.compare.CompareRequestDto;
import com.bioinformatics.dashboard.model.analytics.compare.CompareResponseDto;
import com.bioinformatics.dashboard.model.gene.GeneSearchRequest;

import java.util.List;

public interface FilteredAnalyticsService extends Provider {

    DashboardKpisDto getDashboardKpis(GeneSearchRequest request);

    List<LengthHistogramBucketDto> getLengthHistogram(GeneSearchRequest request);

    List<OrganismCountDto> getByOrganism(int limit, GeneSearchRequest request);

    List<ReviewedRatioDto> getReviewedRatio(GeneSearchRequest request);

    List<EvidenceDistributionDto> getEvidenceLevels(GeneSearchRequest request);

    List<KeywordFrequencyDto> getKeywordFrequency(int limit, GeneSearchRequest request);

    List<ProteinLengthWeightCount> getProteinLengthWeightCount(GeneSearchRequest request);

    CompareResponseDto compare(CompareRequestDto request);
}
