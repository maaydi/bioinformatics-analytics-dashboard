package com.bioinformatics.analyticsservice.providers.dispatcher;

import com.bioinformatics.analyticsservice.interfaces.AnalyticsService;
import com.bioinformatics.analyticsservice.models.*;
import com.bioinformatics.common.providers.AbstractProviderDispatcher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Dispatcher for AnalyticsService implementations.
 *
 * <p>Routes all analytics operations to the active provider based on ProviderContextHolder.
 * Marked as @Primary so controllers inject this dispatcher instead of concrete implementations.
 *
 * <p>Delegates all methods to the active provider (currently PostgreSQL, but can be extended
 * to MongoDB, RDF, or other sources).
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
     *
     * @return dashboard KPIs from active provider
     */
    @Override
    public DashboardKpisDto getDashboardKpis() {
        return resolve().getDashboardKpis();
    }

    /**
     * Delegate getLengthHistogram to active provider.
     *
     * @return length histogram buckets from active provider
     */
    @Override
    public List<LengthHistogramBucketDto> getLengthHistogram() {
        return resolve().getLengthHistogram();
    }

    /**
     * Delegate getByOrganism to active provider.
     *
     * @param limit maximum organisms to return
     * @return organism counts from active provider
     */
    @Override
    public List<OrganismCountDto> getByOrganism(int limit) {
        return resolve().getByOrganism(limit);
    }

    /**
     * Delegate getReviewedRatio to active provider.
     *
     * @return reviewed/unreviewed ratios from active provider
     */
    @Override
    public List<ReviewedRatioDto> getReviewedRatio() {
        return resolve().getReviewedRatio();
    }

    /**
     * Delegate getEvidenceLevels to active provider.
     *
     * @return evidence level distribution from active provider
     */
    @Override
    public List<EvidenceDistributionDto> getEvidenceLevels() {
        return resolve().getEvidenceLevels();
    }

    /**
     * Delegate getKeywordFrequency to active provider.
     *
     * @param limit maximum keywords to return
     * @return keyword frequencies from active provider
     */
    @Override
    public List<KeywordFrequencyDto> getKeywordFrequency(int limit) {
        return resolve().getKeywordFrequency(limit);
    }

}
