package com.bioinformatics.analyticsservice.providers.postgres.service;

import com.bioinformatics.analyticsservice.interfaces.FilteredAnalyticsService;
import com.bioinformatics.analyticsservice.models.*;
import com.bioinformatics.analyticsservice.models.compare.CompareRequestDto;
import com.bioinformatics.analyticsservice.models.compare.CompareResponseDto;
import com.bioinformatics.analyticsservice.providers.postgres.repository.AnalyticsProteinRepository;
import com.bioinformatics.common.gene.specification.GeneSpecification;
import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.common.providers.postgres.AbstractPostgresProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for dynamically querying analytics.
 * Evaluates JPA criteria over core tables instead of static materialized views.
 * Essential for runtime reporting driven by multi-parameter user filters.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PostgresFilteredAnalyticsService extends AbstractPostgresProvider implements FilteredAnalyticsService {

    private final AnalyticsProteinRepository analyticsProteinRepository;

    /**
     * @param request the parameters slicing the target subset
     * @return current dashboard KPIs computed exclusively against the dynamically filtered subset.
     */
    @Override
    @Cacheable(value = "filtered-dashboardKpis", key = "#request.toString()", cacheManager = "redisNonFinalAndRecordCacheManager")
    public DashboardKpisDto getDashboardKpis(GeneSearchRequest request) {
        log.info("Retrieving Kpis for filtered analytics request: {}", request);
        var spec = GeneSpecification.fromRequest(request);
        return analyticsProteinRepository.getDashboardKpis(spec);
    }

    /**
     * @param request the parameters slicing the target subset
     * @return bucketed length frequency natively mapped via filtered query.
     */
    @Override
    @Cacheable(value = "filtered-lengthHistogram", key = "#request.toString()")
    public List<LengthHistogramBucketDto> getLengthHistogram(GeneSearchRequest request) {
        log.info("Retrieving length histogram for filtered analytics request: {}", request);
        var spec = GeneSpecification.fromRequest(request);
        return analyticsProteinRepository.getLengthHistogram(spec);
    }

    /**
     * @param limit   restricts response sizing
     * @param request the parameters slicing the target subset
     * @return highest-occurring mapped organisms restricted to matched dataset context.
     */
    @Override
    @Cacheable(value = "filtered-byOrganism", key = "#request.toString() + '-' + #limit")
    public List<OrganismCountDto> getByOrganism(int limit, GeneSearchRequest request) {
        log.info("Retrieving organism count for filtered analytics request: {}", request);
        var spec = GeneSpecification.fromRequest(request);
        return analyticsProteinRepository.getByOrganism(limit, spec);
    }

    /**
     * @param request the parameters slicing the target subset
     * @return verified vs experimental distribution constrained to current search context.
     */
    @Override
    @Cacheable(value = "filtered-reviewedRatio", key = "#request.toString()")
    public List<ReviewedRatioDto> getReviewedRatio(GeneSearchRequest request) {
        log.info("Retrieving reviewed ratio for filtered analytics request: {}", request);
        var spec = GeneSpecification.fromRequest(request);
        return analyticsProteinRepository.getReviewedRatio(spec);
    }

    /**
     * @param request the parameters slicing the target subset
     * @return distribution grouped by discovery evidence confirmation bounded dynamically.
     */
    @Override
    @Cacheable(value = "filtered-evidenceLevels", key = "#request.toString()")
    public List<EvidenceDistributionDto> getEvidenceLevels(GeneSearchRequest request) {
        log.info("Retrieving evidence levels for filtered analytics request: {}", request);
        var spec = GeneSpecification.fromRequest(request);
        return analyticsProteinRepository.getEvidenceLevels(spec);
    }

    /**
     * @param limit   restricts response sizing
     * @param request the parameters slicing the target subset
     * @return frequent attributes present solely in returned filter dataset matrix.
     */
    @Override
    @Cacheable(value = "filtered-keywordFrequency", key = "#request.toString() + '-' + #limit")
    public List<KeywordFrequencyDto> getKeywordFrequency(int limit, GeneSearchRequest request) {
        log.info("Retrieving keyword frequency for filtered analytics request: {}", request);
        var spec = GeneSpecification.fromRequest(request);
        return analyticsProteinRepository.getKeywordFrequency(limit, spec);
    }

    /**
     * @param request the parameters slicing the target subset
     * @return direct, bucketed sequence weight distributions for accurate mathematical representation of active filters.
     */
    @Override
    @Cacheable(value = "filtered-proteinLengthWeightCount", key = "#request.toString()")
    public List<ProteinLengthWeightCount> getProteinLengthWeightCount(GeneSearchRequest request) {
        log.info("Retrieving protein length frequency for filtered analytics request: {}", request);
        var spec = GeneSpecification.fromRequest(request);
        return analyticsProteinRepository.getProteinLengthWeightCount(spec);
    }

    /**
     * Computes full analytics block side-by-side.
     * Optimizes performance by parallel caching underlying slices whenever possible.
     *
     * @param request encapsulates subsets A and B
     * @return paired comparison analysis structure
     */
    @Override
    @Cacheable(value = "filtered-compareAnalytics", key = "#request.setA().toString() + '-' + #request.setB().toString()")
    public CompareResponseDto compare(CompareRequestDto request) {
        log.info("Compare two filter sets : Filter A : {} | Filter B : {}", request.setA(), request.setB());
        var specA = GeneSpecification.fromRequest(request.setA());
        var specB = GeneSpecification.fromRequest(request.setB());
        var setA = analyticsProteinRepository.getAnalyticsSubset(specA);
        var setB = analyticsProteinRepository.getAnalyticsSubset(specB);
        return new CompareResponseDto(setA, setB);

    }
}
