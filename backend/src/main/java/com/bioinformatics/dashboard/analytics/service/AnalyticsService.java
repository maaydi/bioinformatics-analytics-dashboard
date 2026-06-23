package com.bioinformatics.dashboard.analytics.service;

import com.bioinformatics.dashboard.analytics.dto.*;
import com.bioinformatics.dashboard.analytics.mapper.*;
import com.bioinformatics.dashboard.analytics.repository.*;
import com.bioinformatics.dashboard.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for analytics queries.
 *
 * <p>All methods query pre-computed PostgreSQL materialized views.
 * Views are refreshed post-import by {@code UniProtImportJobConfig}.
 *
 * @see <a href="{@docRoot}/documentation/domain-model.md">Materialized Views</a>
 * @see <a href="{@docRoot}/documentation/api-contract.md">Analytics Endpoints</a>
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

    public DashboardKpisDto getDashboardKpis() {
        log.info("Retrieving Dashboard KPIs from materialized view");
        var entity = dashboardKpisRepository.findFirstBy()
                .orElseThrow(() -> new ResourceNotFoundException("Dashboard KPIs not found"));
        return dashboardKpisMapper.toDto(entity);
    }

    public List<LengthHistogramBucketDto> getLengthHistogram() {
        log.info("Retrieving Length Histogram from materialized view");
        return lengthHistogramBucketRepository.findAllByOrderByBucketAsc()
                .stream()
                .map(lengthHistogramBucketMapper::toDto)
                .toList();
    }

    public List<OrganismCountDto> getByOrganism(int limit) {
        log.info("Retrieving Organism Count from materialized view");
        return organismCountRepository.findAll(Limit.of(limit))
                .stream()
                .map(organismCountMapper::toDto)
                .toList();
    }

    public List<ReviewedRatioDto> getReviewedRatio() {
        log.info("Retrieving Reviewed Ratio from materialized view");
        return reviewedRatioRepository.findAll()
                .stream()
                .map(reviewedRatioMapper::toDto)
                .toList();
    }

    public List<EvidenceDistributionDto> getEvidenceLevels() {
        log.info("Retrieving Evidence Levels from materialized view");
        return evidenceDistributionRepository.findAll()
                .stream()
                .map(evidenceDistributionMapper::toDto)
                .toList();
    }

    public List<KeywordFrequencyDto> getKeywordFrequency(int limit) {
        log.info("Retrieving Keyword Frequency from materialized view");
        return keywordFrequencyRepository.findAll(Limit.of(limit))
                .stream()
                .map(keywordFrequencyMapper::toDto)
                .toList();
    }
}
