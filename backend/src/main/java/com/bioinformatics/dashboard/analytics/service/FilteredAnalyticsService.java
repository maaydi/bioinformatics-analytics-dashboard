package com.bioinformatics.dashboard.analytics.service;

import com.bioinformatics.dashboard.analytics.dto.*;
import com.bioinformatics.dashboard.analytics.dto.compare.CompareRequestDto;
import com.bioinformatics.dashboard.analytics.dto.compare.CompareResponseDto;
import com.bioinformatics.dashboard.gene.dto.GeneSearchRequest;
import com.bioinformatics.dashboard.gene.repository.ProteinEntryRepository;
import com.bioinformatics.dashboard.gene.specification.GeneSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for dynamically querying analytics.
 * Evaluates JPA criteria over core tables instead of static materialized views.
 * Essential for runtime reporting driven by multi-parameter user filters.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FilteredAnalyticsService {

    private final ProteinEntryRepository proteinEntryRepository;

    /**
     * @param request the parameters slicing the target subset
     * @return current dashboard KPIs computed exclusively against the dynamically filtered subset.
     */
    @Cacheable(value = "filtered-dashboardKpis", key = "#request.toString()", cacheManager = "redisNonFinalAndRecordCacheManager")
    public DashboardKpisDto getDashboardKpis(GeneSearchRequest request) {
        log.info("Retrieving Kpis for filtered analytics request: {}", request);
        var spec = GeneSpecification.fromRequest(request);
        return proteinEntryRepository.getDashboardKpis(spec);
    }

    /**
     * @param request the parameters slicing the target subset
     * @return bucketed length frequency natively mapped via filtered query.
     */
    @Cacheable(value = "filtered-lengthHistogram", key = "#request.toString()")
    public List<LengthHistogramBucketDto> getLengthHistogram(GeneSearchRequest request) {
        log.info("Retrieving length histogram for filtered analytics request: {}", request);
        var spec = GeneSpecification.fromRequest(request);
        return proteinEntryRepository.getLengthHistogram(spec);
    }

    /**
     * @param limit   restricts response sizing
     * @param request the parameters slicing the target subset
     * @return highest-occurring mapped organisms restricted to matched dataset context.
     */
    @Cacheable(value = "filtered-byOrganism", key = "#request.toString() + '-' + #limit")
    public List<OrganismCountDto> getByOrganism(int limit, GeneSearchRequest request) {
        log.info("Retrieving organism count for filtered analytics request: {}", request);
        var spec = GeneSpecification.fromRequest(request);
        return proteinEntryRepository.getByOrganism(limit, spec);
    }

    /**
     * @param request the parameters slicing the target subset
     * @return verified vs experimental distribution constrained to current search context.
     */
    @Cacheable(value = "filtered-reviewedRatio", key = "#request.toString()")
    public List<ReviewedRatioDto> getReviewedRatio(GeneSearchRequest request) {
        log.info("Retrieving reviewed ratio for filtered analytics request: {}", request);
        var spec = GeneSpecification.fromRequest(request);
        return proteinEntryRepository.getReviewedRatio(spec);
    }

    /**
     * @param request the parameters slicing the target subset
     * @return distribution grouped by discovery evidence confirmation bounded dynamically.
     */
    @Cacheable(value = "filtered-evidenceLevels", key = "#request.toString()")
    public List<EvidenceDistributionDto> getEvidenceLevels(GeneSearchRequest request) {
        log.info("Retrieving evidence levels for filtered analytics request: {}", request);
        var spec = GeneSpecification.fromRequest(request);
        return proteinEntryRepository.getEvidenceLevels(spec);
    }

    /**
     * @param limit restricts response sizing
     * @param request the parameters slicing the target subset
     * @return frequent attributes present solely in returned filter dataset matrix.
     */
    @Cacheable(value = "filtered-keywordFrequency", key = "#request.toString() + '-' + #limit")
    public List<KeywordFrequencyDto> getKeywordFrequency(int limit, GeneSearchRequest request) {
        log.info("Retrieving keyword frequency for filtered analytics request: {}", request);
        var spec = GeneSpecification.fromRequest(request);
        return proteinEntryRepository.getKeywordFrequency(limit, spec);
    }

    /**
     * @param request the parameters slicing the target subset
     * @return direct, unbucketed sequence weight distributions for accurate mathematical representation of active filters.
     */
    @Cacheable(value = "filtered-proteinLengthWeightCount", key = "#request.toString()")
    public List<ProteinLengthWeightCount> getProteinLengthWeightCount(GeneSearchRequest request) {
        log.info("Retrieving protein length frequency for filtered analytics request: {}", request);
        var spec = GeneSpecification.fromRequest(request);
        return proteinEntryRepository.getProteinLengthWeightCount(spec);
    }

    /**
     * Computes full analytics block side-by-side.
     * Optimizes performance by parallel caching underlying slices whenever possible.
     *
     * @param request encapsulates subsets A and B
     * @return paired comparison analysis structure
     */
    @Cacheable(value = "filtered-compareAnalytics", key = "#request.setA().toString() + '-' + #request.setB().toString()")
    public CompareResponseDto compare(CompareRequestDto request) {
        log.info("Compare two filter sets : Filter A : {} | Filter B : {}", request.setA(), request.setB());
        var specA = GeneSpecification.fromRequest(request.setA());
        var specB = GeneSpecification.fromRequest(request.setB());
        var setA = proteinEntryRepository.getAnalyticsSubset(specA);
        var setB = proteinEntryRepository.getAnalyticsSubset(specB);
        return new CompareResponseDto(setA, setB);

    }
}
