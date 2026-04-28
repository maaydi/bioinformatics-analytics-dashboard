package com.bioinformatics.dashboard.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for analytics chart endpoints.
 *
 * <p>All endpoints are served from pre-computed PostgreSQL materialized views.
 * See documentation/api-contract.md §2 — Analytics Endpoints.
 *
 * <ul>
 *   <li>{@code GET /api/analytics/dashboard-kpis}       — mv_dashboard_kpis</li>
 *   <li>{@code GET /api/analytics/length-histogram}     — mv_length_histogram</li>
 *   <li>{@code GET /api/analytics/by-organism}          — mv_organism_counts</li>
 *   <li>{@code GET /api/analytics/reviewed-ratio}       — mv_reviewed_ratio</li>
 *   <li>{@code GET /api/analytics/evidence-levels}      — mv_evidence_distribution</li>
 *   <li>{@code GET /api/analytics/keyword-frequency}    — mv_keyword_frequency</li>
 * </ul>
 *
 * <p>Authorization: USER and ADMIN.
 * Response time target: ≤ 500 ms (NFR §12.1).
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    // TODO: inject AnalyticsService

    @GetMapping("/dashboard-kpis")
    public ResponseEntity<?> getDashboardKpis() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @GetMapping("/length-histogram")
    public ResponseEntity<?> getLengthHistogram() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @GetMapping("/by-organism")
    public ResponseEntity<?> getByOrganism(
            @RequestParam(defaultValue = "50") int limit) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @GetMapping("/reviewed-ratio")
    public ResponseEntity<?> getReviewedRatio() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @GetMapping("/evidence-levels")
    public ResponseEntity<?> getEvidenceLevels() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @GetMapping("/keyword-frequency")
    public ResponseEntity<?> getKeywordFrequency(
            @RequestParam(defaultValue = "100") int limit) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
