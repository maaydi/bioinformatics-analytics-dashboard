package com.bioinformatics.analyticsservice.providers.postgres.service;

import com.bioinformatics.analyticsservice.interfaces.AnalyticsService;
import com.bioinformatics.analyticsservice.models.*;
import com.bioinformatics.analyticsservice.providers.postgres.mapper.*;
import com.bioinformatics.analyticsservice.providers.postgres.repository.*;
import com.bioinformatics.common.exception.ResourceNotFoundException;
import com.bioinformatics.common.providers.postgres.AbstractPostgresProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for static, pre-aggregated analytics queries.
 *
 * <p>Driven strictly by PostgreSQL materialized views for high-performance sub-500ms responses.
 * All methods are cached via Redis or Spring Cache abstractions.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Query materialized views populated by import batch jobs
 *   <li>Map entity results to DTOs
 *   <li>Support caching for sub-100ms response times
 *   <li>Handle cache eviction on data refresh
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PostgresAnalyticsService extends AbstractPostgresProvider implements AnalyticsService {

    private final DashboardKpisRepository dashboardKpisRepository;
    private final DashboardKpisMapper dashboardKpisMapper;

    private final LengthHistogramBucketRepository lengthHistogramBucketRepository;
    private final LengthHistogramBucketMapper lengthHistogramBucketMapper;

    private final OrganismCountRepository organismCountRepository;
    private final OrganismCountMapper organismCountMapper;

    private final ReviewedRatioRepository reviewedRatioRepository;
    private final ReviewedRatioMapper reviewedRatioMapper;

    private final EvidenceDistributionRepository evidenceDistributionRepository;
    private final EvidenceDistributionMapper evidenceDistributionMapper;

    private final KeywordFrequencyRepository keywordFrequencyRepository;
    private final KeywordFrequencyMapper keywordFrequencyMapper;

    /**
     * Retrieves top-level dashboard KPIs from materialized view.
     *
     * <p>Cached in Redis with cacheManager=redisNonFinalAndRecordCacheManager.
     * Query hits: mv_dashboard_kpis.
     *
     * @return dashboard KPIs (total proteins, reviewed count, etc.)
     * @throws ResourceNotFoundException if view is empty
     */
    @Override
    @Cacheable(value = "dashboardKpis", cacheManager = "redisNonFinalAndRecordCacheManager")
    public DashboardKpisDto getDashboardKpis() {
        log.debug("[ANALYTICS] Retrieving Dashboard KPIs from materialized view");
        var entity = dashboardKpisRepository.findFirstBy()
                .orElseThrow(() -> {
                    log.error("[ANALYTICS] Dashboard KPIs not found in materialized view");
                    return new ResourceNotFoundException("Dashboard KPIs not found");
                });
        log.debug("[ANALYTICS] Dashboard KPIs retrieved - totalProteins={}", entity.getTotalProteins());
        return dashboardKpisMapper.toDto(entity);
    }

    /**
     * Retrieves bucketed protein length frequency distribution.
     *
     * <p>Pre-computed buckets for chart rendering (e.g., 0-100, 100-200, etc.).
     * Cached with cache=lengthHistogram.
     * Query hits: mv_length_histogram.
     *
     * @return ordered list of histogram buckets
     */
    @Override
    @Cacheable(value = "lengthHistogram")
    public List<LengthHistogramBucketDto> getLengthHistogram() {
        log.debug("[ANALYTICS] Retrieving Length Histogram from materialized view");
        var buckets = lengthHistogramBucketRepository.findAllByOrderByBucketAsc()
                .stream()
                .map(lengthHistogramBucketMapper::toDto)
                .toList();
        log.debug("[ANALYTICS] Length Histogram retrieved - bucketCount={}", buckets.size());
        return buckets;
    }

    /**
     * Retrieves global occurrences of organisms sorted descending by count.
     *
     * <p>Highest-cardinality domain; limit parameter essential for performance.
     * Cached per limit value with cache=byOrganism, key=#limit.
     * Query hits: mv_organism_counts.
     *
     * @param limit maximum number of organisms to return (1-200)
     * @return top organisms with counts
     */
    @Override
    @Cacheable(value = "byOrganism", key = "#limit")
    public List<OrganismCountDto> getByOrganism(int limit) {
        log.debug("[ANALYTICS] Retrieving Organism Count from materialized view - limit={}", limit);
        var organisms = organismCountRepository.findAll(Limit.of(limit))
                .stream()
                .map(organismCountMapper::toDto)
                .toList();
        log.debug("[ANALYTICS] Organism Count retrieved - resultCount={}", organisms.size());
        return organisms;
    }

    /**
     * Retrieves the ratio of reviewed (UniProt-reviewed) to unreviewed proteins.
     *
     * <p>Tracks verified sequences vs newly discovered entries.
     * Cached with cache=reviewedRatio.
     * Query hits: mv_reviewed_ratio.
     *
     * @return ratio metrics
     */
    @Override
    @Cacheable(value = "reviewedRatio")
    public List<ReviewedRatioDto> getReviewedRatio() {
        log.debug("[ANALYTICS] Retrieving Reviewed Ratio from materialized view");
        var ratios = reviewedRatioRepository.findAll()
                .stream()
                .map(reviewedRatioMapper::toDto)
                .toList();
        log.debug("[ANALYTICS] Reviewed Ratio retrieved - resultCount={}", ratios.size());
        return ratios;
    }

    /**
     * Retrieves distribution of evidence confirmation levels.
     *
     * <p>Maps protein function evidence from experiments to predictions.
     * Cached with cache=evidenceLevels.
     * Query hits: mv_evidence_distribution.
     *
     * @return evidence level distribution
     */
    @Override
    @Cacheable(value = "evidenceLevels")
    public List<EvidenceDistributionDto> getEvidenceLevels() {
        log.debug("[ANALYTICS] Retrieving Evidence Levels from materialized view");
        var distribution = evidenceDistributionRepository.findAll()
                .stream()
                .map(evidenceDistributionMapper::toDto)
                .toList();
        log.debug("[ANALYTICS] Evidence Levels retrieved - resultCount={}", distribution.size());
        return distribution;
    }

    /**
     * Retrieves most frequently occurring keywords across dataset.
     *
     * <p>Useful for tag clouds and faceted search.
     * Highest-cardinality dictionary; limit parameter essential.
     * Cached per limit value with cache=keywordFrequency, key=#limit.
     * Query hits: mv_keyword_frequency.
     *
     * @param limit maximum keywords to return (1-500)
     * @return top keywords with occurrence counts
     */
    @Override
    @Cacheable(value = "keywordFrequency", key = "#limit")
    public List<KeywordFrequencyDto> getKeywordFrequency(int limit) {
        log.debug("[ANALYTICS] Retrieving Keyword Frequency from materialized view - limit={}", limit);
        var keywords = keywordFrequencyRepository.findAll(Limit.of(limit))
                .stream()
                .map(keywordFrequencyMapper::toDto)
                .toList();
        log.debug("[ANALYTICS] Keyword Frequency retrieved - resultCount={}", keywords.size());
        return keywords;
    }
}
