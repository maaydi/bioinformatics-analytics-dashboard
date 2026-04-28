package com.bioinformatics.dashboard.analytics.service;

/**
 * Service interface for analytics queries.
 *
 * <p>All methods query pre-computed PostgreSQL materialized views.
 * Views are refreshed post-import by {@code UniProtImportJobConfig}.
 *
 * @see documentation/domain-model.md §11 — Materialized Views
 * @see documentation/api-contract.md §2 — Analytics Endpoints
 */
public interface AnalyticsService {

    // TODO: define return DTOs matching api-contract.md §2 schemas before implementing

    Object getDashboardKpis();
    Object getLengthHistogram();
    Object getByOrganism(int limit);
    Object getReviewedRatio();
    Object getEvidenceLevels();
    Object getKeywordFrequency(int limit);
}
