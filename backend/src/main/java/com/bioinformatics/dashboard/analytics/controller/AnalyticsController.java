package com.bioinformatics.dashboard.analytics.controller;

import com.bioinformatics.dashboard.analytics.dto.*;
import com.bioinformatics.dashboard.analytics.service.AnalyticsService;
import com.bioinformatics.dashboard.audit.annotation.Auditable;
import com.bioinformatics.dashboard.audit.annotation.RateLimited;
import com.bioinformatics.dashboard.audit.dto.AuditAction;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
@Validated
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','USER')")
public class AnalyticsController {

    private final AnalyticsService service;

    @GetMapping("/dashboard-kpis")
    @RateLimited(key = "analysis")
    @Auditable(action = AuditAction.DETAIL_VIEW)
    public ResponseEntity<DashboardKpisDto> getDashboardKpis() {
        var kpis = service.getDashboardKpis();
        return ResponseEntity.ok(kpis);
    }

    @GetMapping("/length-histogram")
    @RateLimited(key = "analysis")
    @Auditable(action = AuditAction.DETAIL_VIEW)
    public ResponseEntity<List<LengthHistogramBucketDto>> getLengthHistogram() {
        var buckets = service.getLengthHistogram();
        return ResponseEntity.ok(buckets);
    }

    @GetMapping("/by-organism")
    @RateLimited(key = "analysis")
    @Auditable(action = AuditAction.DETAIL_VIEW)
    public ResponseEntity<List<OrganismCountDto>> getByOrganism(
            @Min(value = 1, message = "Limit should be greater than 0")
            @Max(value = 200, message = "Limit should be lower than 201")
            @RequestParam(defaultValue = "50") int limit) {
        var count = service.getByOrganism(limit);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/reviewed-ratio")
    @RateLimited(key = "analysis")
    @Auditable(action = AuditAction.DETAIL_VIEW)
    public ResponseEntity<List<ReviewedRatioDto>> getReviewedRatio() {
        var ratios = service.getReviewedRatio();
        return ResponseEntity.ok(ratios);
    }

    @GetMapping("/evidence-levels")
    @RateLimited(key = "analysis")
    @Auditable(action = AuditAction.DETAIL_VIEW)
    public ResponseEntity<List<EvidenceDistributionDto>> getEvidenceLevels() {
        var ev = service.getEvidenceLevels();
        return ResponseEntity.ok(ev);
    }

    @GetMapping("/keyword-frequency")
    @RateLimited(key = "analysis")
    @Auditable(action = AuditAction.DETAIL_VIEW)
    public ResponseEntity<List<KeywordFrequencyDto>> getKeywordFrequency(
            @Min(value = 1, message = "Limit should be greater than 0")
            @Max(value = 500, message = "Limit should be lower than 501")
            @RequestParam(defaultValue = "100") int limit) {
        var keywords = service.getKeywordFrequency(limit);
        return ResponseEntity.ok(keywords);
    }
}
