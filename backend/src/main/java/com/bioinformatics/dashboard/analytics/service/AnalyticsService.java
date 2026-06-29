package com.bioinformatics.dashboard.analytics.service;

import com.bioinformatics.dashboard.analytics.dto.*;
import com.bioinformatics.dashboard.analytics.mapper.*;
import com.bioinformatics.dashboard.analytics.repository.*;
import com.bioinformatics.dashboard.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for statically pre-aggregated analytics queries.
 * Driven strictly by PostgreSQL materialized views for high-performance (sub-500ms target).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

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
     * @return current dashboard KPIs from cache or materialized record.
     */
    @Cacheable(value = "dashboardKpis", cacheManager = "redisNonFinalAndRecordCacheManager")
    public DashboardKpisDto getDashboardKpis() {
        log.info("Retrieving Dashboard KPIs from materialized view");
        var entity = dashboardKpisRepository.findFirstBy()
                .orElseThrow(() -> new ResourceNotFoundException("Dashboard KPIs not found"));
        return dashboardKpisMapper.toDto(entity);
    }

    /**
     * @return bucketed length frequency map natively computed in DB.
     */
    @Cacheable(value = "lengthHistogram")
    public List<LengthHistogramBucketDto> getLengthHistogram() {
        log.info("Retrieving Length Histogram from materialized view");
        return lengthHistogramBucketRepository.findAllByOrderByBucketAsc()
                .stream()
                .map(lengthHistogramBucketMapper::toDto)
                .toList();
    }

    /**
     * @param limit limits response size for high-cardinality taxa mapping
     * @return global occurrences of organisms sorted descending.
     */
    @Cacheable(value = "byOrganism", key = "#limit")
    public List<OrganismCountDto> getByOrganism(int limit) {
        log.info("Retrieving Organism Count from materialized view");
        return organismCountRepository.findAll(Limit.of(limit))
                .stream()
                .map(organismCountMapper::toDto)
                .toList();
    }

    /**
     * @return ratio tracking verified vs newly-found sequences.
     */
    @Cacheable(value = "reviewedRatio")
    public List<ReviewedRatioDto> getReviewedRatio() {
        log.info("Retrieving Reviewed Ratio from materialized view");
        return reviewedRatioRepository.findAll()
                .stream()
                .map(reviewedRatioMapper::toDto)
                .toList();
    }

    /**
     * @return distribution grouped by evidence confirmation level.
     */
    @Cacheable(value = "evidenceLevels")
    public List<EvidenceDistributionDto> getEvidenceLevels() {
        log.info("Retrieving Evidence Levels from materialized view");
        return evidenceDistributionRepository.findAll()
                .stream()
                .map(evidenceDistributionMapper::toDto)
                .toList();
    }

    /**
     * @param limit limits response size for massive dictionary graphs
     * @return common trait occurrences over entire domain dataset.
     */
    @Cacheable(value = "keywordFrequency", key = "#limit")
    public List<KeywordFrequencyDto> getKeywordFrequency(int limit) {
        log.info("Retrieving Keyword Frequency from materialized view");
        return keywordFrequencyRepository.findAll(Limit.of(limit))
                .stream()
                .map(keywordFrequencyMapper::toDto)
                .toList();
    }
}
