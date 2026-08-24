package com.bioinformatics.analyticsservice.providers.dispatcher;

import com.bioinformatics.analyticsservice.interfaces.FilteredAnalyticsService;
import com.bioinformatics.analyticsservice.models.*;
import com.bioinformatics.analyticsservice.models.compare.CompareRequestDto;
import com.bioinformatics.analyticsservice.models.compare.CompareResponseDto;
import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.common.providers.AbstractProviderDispatcher;
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
