package com.bioinformatics.analyticsservice.providers.dispatcher;

import com.bioinformatics.analyticsservice.interfaces.AnalyticsService;
import com.bioinformatics.analyticsservice.models.*;
import com.bioinformatics.common.providers.AbstractProviderDispatcher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Dispatcher for AnalyticsService implementations.
 * Routes all analytics operations to the active provider based on ProviderContextHolder.
 * Marked as @Primary so controllers inject this dispatcher instead of concrete implementations.
 */
@Service
@Primary
public class AnalyticsServiceDispatcher extends AbstractProviderDispatcher<AnalyticsService> implements AnalyticsService {

    /**
     * Initialize dispatcher with all registered AnalyticsService implementations.
     *
     * @param services all AnalyticsService beans (postgres, mongo, rdf, etc.)
     */
    public AnalyticsServiceDispatcher(List<AnalyticsService> services) {
        super(services);
    }

    /**
     * Delegate getDashboardKpis to active provider.
     */
    @Override
    public DashboardKpisDto getDashboardKpis() {
        return resolve().getDashboardKpis();
    }

    /**
     * Delegate getLengthHistogram to active provider.
     */
    @Override
    public List<LengthHistogramBucketDto> getLengthHistogram() {
        return resolve().getLengthHistogram();
    }

    /**
     * Delegate getByOrganism to active provider.
     */
    @Override
    public List<OrganismCountDto> getByOrganism(int limit) {
        return resolve().getByOrganism(limit);
    }

    /**
     * Delegate getReviewedRatio to active provider.
     */
    @Override
    public List<ReviewedRatioDto> getReviewedRatio() {
        return resolve().getReviewedRatio();
    }

    /**
     * Delegate getEvidenceLevels to active provider.
     */
    @Override
    public List<EvidenceDistributionDto> getEvidenceLevels() {
        return resolve().getEvidenceLevels();
    }

    /**
     * Delegate getKeywordFrequency to active provider.
     */
    @Override
    public List<KeywordFrequencyDto> getKeywordFrequency(int limit) {
        return resolve().getKeywordFrequency(limit);
    }

}
