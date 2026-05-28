package com.bioinformatics.dashboard.analytics.service;

import com.bioinformatics.dashboard.analytics.dto.*;
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
public class AnalyticsService {

    // TODO: define return DTOs matching api-contract.md §2 schemas before implementing

    public DashboardKpisDto getDashboardKpis() {
        // TODO
        return null;
    }

    public List<LengthBucketDto> getLengthHistogram() {
        // TODO
        return List.of();
    }

    public List<OrganismCountDto> getByOrganism(int limit) {
        // TODO
        return List.of();
    }

    public List<ReviewedRatioDto> getReviewedRatio() {
        // TODO
        return List.of();
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
