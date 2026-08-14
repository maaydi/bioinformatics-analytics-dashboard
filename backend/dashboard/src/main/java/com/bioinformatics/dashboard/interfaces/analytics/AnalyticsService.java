package com.bioinformatics.dashboard.interfaces.analytics;

import com.bioinformatics.dashboard.interfaces.Provider;
import com.bioinformatics.dashboard.model.analytics.*;

import java.util.List;

/**
 * Provider contract for aggregate analytics and reporting operations.
 * Implementations provide KPIs, histograms, and distribution metrics for dashboard visualization.
 */
public interface AnalyticsService extends Provider {

    /**
     * Fetch dashboard KPI summary (total count, average, etc.).
     *
     * @return aggregated KPI metrics
     */
    DashboardKpisDto getDashboardKpis();

    /**
     * Fetch protein length distribution bucketed into histogram bins.
     * @return list of histogram buckets with counts
     */
    List<LengthHistogramBucketDto> getLengthHistogram();

    /**
     * Fetch top organisms by protein count.
     * @param limit max number of organisms to return
     * @return ordered list of organism counts
     */
    List<OrganismCountDto> getByOrganism(int limit);

    /**
     * Fetch reviewed vs unreviewed ratio.
     * @return ratio distribution data
     */
    List<ReviewedRatioDto> getReviewedRatio();

    /**
     * Fetch evidence level distribution (experimental, computational, etc.).
     * @return distribution breakdown by evidence type
     */
    List<EvidenceDistributionDto> getEvidenceLevels();

    /**
     * Fetch most frequent protein keywords/tags.
     * @param limit max keywords to return
     * @return ordered keyword frequency list
     */
    List<KeywordFrequencyDto> getKeywordFrequency(int limit);
}
