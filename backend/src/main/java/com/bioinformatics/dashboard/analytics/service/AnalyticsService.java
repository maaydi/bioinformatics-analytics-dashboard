package com.bioinformatics.dashboard.analytics.service;

import com.bioinformatics.dashboard.analytics.dto.*;
import com.bioinformatics.dashboard.analytics.mapper.DashboardKpisMapper;
import com.bioinformatics.dashboard.analytics.mapper.LengthHistogramBucketMapper;
import com.bioinformatics.dashboard.analytics.mapper.OrganismCountMapper;
import com.bioinformatics.dashboard.analytics.mapper.ReviewedRatioMapper;
import com.bioinformatics.dashboard.analytics.repository.DashboardKpisRepository;
import com.bioinformatics.dashboard.analytics.repository.LengthHistogramBucketRepository;
import com.bioinformatics.dashboard.analytics.repository.OrganismCountRepository;
import com.bioinformatics.dashboard.analytics.repository.ReviewedRatioRepository;
import com.bioinformatics.dashboard.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
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
public class AnalyticsService {

    private final DashboardKpisRepository dashboardKpisRepository;
    private final DashboardKpisMapper dashboardKpisMapper;

    private final LengthHistogramBucketRepository lengthHistogramBucketRepository;
    private final LengthHistogramBucketMapper lengthHistogramBucketMapper;

    private final OrganismCountRepository organismCountRepository;
    private final OrganismCountMapper organismCountMapper;

    private final ReviewedRatioRepository reviewedRatioRepository;
    private final ReviewedRatioMapper reviewedRatioMapper;

    public DashboardKpisDto getDashboardKpis() {
        var entity = dashboardKpisRepository.findFirstBy()
                .orElseThrow(() -> new ResourceNotFoundException("Dashboard KPIs not found"));
        return dashboardKpisMapper.toDto(entity);
    }

    public List<LengthHistogramBucketDto> getLengthHistogram() {
        return lengthHistogramBucketRepository.findAllByOrderByBucketAsc()
                .stream()
                .map(lengthHistogramBucketMapper::toDto)
                .toList();
    }

    public List<OrganismCountDto> getByOrganism(int limit) {
        return organismCountRepository.findAll(Limit.of(limit))
                .stream()
                .map(organismCountMapper::toDto)
                .toList();
    }

    public List<ReviewedRatioDto> getReviewedRatio() {
        return reviewedRatioRepository.findAll()
                .stream()
                .map(reviewedRatioMapper::toDto)
                .toList();
    }

    public List<EvidenceLevelDto> getEvidenceLevels() {
        // TODO
        return List.of();
    }

    public List<KeywordFrequencyDto> getKeywordFrequency(int limit) {
        // TODO
        return List.of();
    }
}
