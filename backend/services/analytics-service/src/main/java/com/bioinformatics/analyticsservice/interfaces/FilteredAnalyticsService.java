package com.bioinformatics.analyticsservice.interfaces;

import com.bioinformatics.analyticsservice.models.*;
import com.bioinformatics.analyticsservice.models.compare.CompareRequestDto;
import com.bioinformatics.analyticsservice.models.compare.CompareResponseDto;
import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.common.providers.Provider;

import java.util.List;

/**
 * Provider contract for filtered analytics operations.
 * Extends AnalyticsService with gene search filter support to enable filtered aggregations and comparisons.
 */
public interface FilteredAnalyticsService extends Provider {

    /**
     * Fetch dashboard KPI summary for genes matching the filter.
     *
     * @param request gene search/filter criteria
     * @return filtered KPI metrics
     */
    DashboardKpisDto getDashboardKpis(GeneSearchRequest request);

    /**
     * Fetch length histogram for filtered genes.
     *
     * @param request gene search/filter criteria
     * @return filtered length distribution buckets
     */
    List<LengthHistogramBucketDto> getLengthHistogram(GeneSearchRequest request);

    /**
     * Fetch organisms distribution for filtered genes.
     *
     * @param limit   max organisms to return
     * @param request gene search/filter criteria
     * @return filtered organism counts
     */
    List<OrganismCountDto> getByOrganism(int limit, GeneSearchRequest request);

    /**
     * Fetch reviewed ratio for filtered genes.
     *
     * @param request gene search/filter criteria
     * @return filtered reviewed distribution
     */
    List<ReviewedRatioDto> getReviewedRatio(GeneSearchRequest request);

    /**
     * Fetch evidence levels for filtered genes.
     *
     * @param request gene search/filter criteria
     * @return filtered evidence distribution
     */
    List<EvidenceDistributionDto> getEvidenceLevels(GeneSearchRequest request);

    /**
     * Fetch keyword frequency for filtered genes.
     *
     * @param limit   max keywords to return
     * @param request gene search/filter criteria
     * @return filtered keyword frequency list
     */
    List<KeywordFrequencyDto> getKeywordFrequency(int limit, GeneSearchRequest request);

    /**
     * Fetch protein length vs molecular weight scatter plot data for filtered genes.
     *
     * @param request gene search/filter criteria
     * @return list of length-weight coordinate pairs with counts
     */
    List<ProteinLengthWeightCount> getProteinLengthWeightCount(GeneSearchRequest request);

    /**
     * Compare analytics of two filtered gene sets.
     *
     * @param request specification of two gene filters to compare
     * @return comparative analysis of both sets
     */
    CompareResponseDto compare(CompareRequestDto request);
}
