package com.bioinformatics.dashboard.providers.dispatcher;

import com.bioinformatics.dashboard.interfaces.analytics.FilteredAnalyticsService;
import com.bioinformatics.dashboard.model.analytics.*;
import com.bioinformatics.dashboard.model.analytics.compare.CompareRequestDto;
import com.bioinformatics.dashboard.model.analytics.compare.CompareResponseDto;
import com.bioinformatics.dashboard.model.gene.GeneSearchRequest;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Primary
public class FilteredAnalyticsServiceDispatcher extends AbstractProviderDispatcher<FilteredAnalyticsService> implements FilteredAnalyticsService {

    public FilteredAnalyticsServiceDispatcher(List<FilteredAnalyticsService> services) {
        super(services);
    }

    @Override
    public DashboardKpisDto getDashboardKpis(GeneSearchRequest request) {
        return resolve().getDashboardKpis(request);
    }

    @Override
    public List<LengthHistogramBucketDto> getLengthHistogram(GeneSearchRequest request) {
        return resolve().getLengthHistogram(request);
    }

    @Override
    public List<OrganismCountDto> getByOrganism(int limit, GeneSearchRequest request) {
        return resolve().getByOrganism(limit, request);
    }

    @Override
    public List<ReviewedRatioDto> getReviewedRatio(GeneSearchRequest request) {
        return resolve().getReviewedRatio(request);
    }

    @Override
    public List<EvidenceDistributionDto> getEvidenceLevels(GeneSearchRequest request) {
        return resolve().getEvidenceLevels(request);
    }

    @Override
    public List<KeywordFrequencyDto> getKeywordFrequency(int limit, GeneSearchRequest request) {
        return resolve().getKeywordFrequency(limit, request);
    }

    @Override
    public List<ProteinLengthWeightCount> getProteinLengthWeightCount(GeneSearchRequest request) {
        return resolve().getProteinLengthWeightCount(request);
    }

    @Override
    public CompareResponseDto compare(CompareRequestDto request) {
        return resolve().compare(request);
    }

}
