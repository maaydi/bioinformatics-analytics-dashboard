package com.bioinformatics.dashboard.providers.dispatcher;

import com.bioinformatics.dashboard.interfaces.analytics.FilteredAnalyticsService;
import com.bioinformatics.dashboard.model.analytics.*;
import com.bioinformatics.dashboard.model.analytics.compare.CompareRequestDto;
import com.bioinformatics.dashboard.model.analytics.compare.CompareResponseDto;
import com.bioinformatics.dashboard.model.gene.GeneSearchRequest;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Dispatcher for FilteredAnalyticsService implementations.
 * Routes all filtered analytics and comparison operations to the active provider based on ProviderContextHolder.
 * Marked as @Primary so controllers inject this dispatcher instead of concrete implementations.
 */
@Service
@Primary
public class FilteredAnalyticsServiceDispatcher extends AbstractProviderDispatcher<FilteredAnalyticsService> implements FilteredAnalyticsService {

    /**
     * Initialize dispatcher with all registered FilteredAnalyticsService implementations.
     *
     * @param services all FilteredAnalyticsService beans (postgres, mongo, rdf, etc.)
     */
    public FilteredAnalyticsServiceDispatcher(List<FilteredAnalyticsService> services) {
        super(services);
    }

    /**
     * Delegate getDashboardKpis to active provider.
     */
    @Override
    public DashboardKpisDto getDashboardKpis(GeneSearchRequest request) {
        return resolve().getDashboardKpis(request);
    }

    /**
     * Delegate getLengthHistogram to active provider.
     */
    @Override
    public List<LengthHistogramBucketDto> getLengthHistogram(GeneSearchRequest request) {
        return resolve().getLengthHistogram(request);
    }

    /**
     * Delegate getByOrganism to active provider.
     */
    @Override
    public List<OrganismCountDto> getByOrganism(int limit, GeneSearchRequest request) {
        return resolve().getByOrganism(limit, request);
    }

    /**
     * Delegate getReviewedRatio to active provider.
     */
    @Override
    public List<ReviewedRatioDto> getReviewedRatio(GeneSearchRequest request) {
        return resolve().getReviewedRatio(request);
    }

    /**
     * Delegate getEvidenceLevels to active provider.
     */
    @Override
    public List<EvidenceDistributionDto> getEvidenceLevels(GeneSearchRequest request) {
        return resolve().getEvidenceLevels(request);
    }

    /**
     * Delegate getKeywordFrequency to active provider.
     */
    @Override
    public List<KeywordFrequencyDto> getKeywordFrequency(int limit, GeneSearchRequest request) {
        return resolve().getKeywordFrequency(limit, request);
    }

    /**
     * Delegate getProteinLengthWeightCount to active provider.
     */
    @Override
    public List<ProteinLengthWeightCount> getProteinLengthWeightCount(GeneSearchRequest request) {
        return resolve().getProteinLengthWeightCount(request);
    }

    /**
     * Delegate compare to active provider.
     */
    @Override
    public CompareResponseDto compare(CompareRequestDto request) {
        return resolve().compare(request);
    }

}
