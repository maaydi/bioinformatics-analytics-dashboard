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
 * REST Controller serving static, highly performant analytics and KPIs.
 *
 * <p>All endpoints within this controller rely heavily on PostgreSQL materialized views
 * populated during the batch import process, allowing for fast, pre-computed dashboard metrics
 * handling hundreds of megabytes of relational data under 500ms.</p>
 *
 * <p>Delegates query retrieval to the {@link AnalyticsService}. Accessible to users
 * holding either USER or ADMIN authority.</p>
 */
@RestController
@Validated
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','USER')")
public class AnalyticsController {

    private final AnalyticsService service;

    /**
     * Retrieves top-level dashboard KPIs.
     */
    @GetMapping("/dashboard-kpis")
    @RateLimited(key = "analysis")
    @Auditable(action = AuditAction.DETAIL_VIEW)
    public ResponseEntity<DashboardKpisDto> getDashboardKpis() {
        var kpis = service.getDashboardKpis();
        return ResponseEntity.ok(kpis);
    }

    /**
     * Retrieves the length distribution histogram buckets.
     */
    @GetMapping("/length-histogram")
    @RateLimited(key = "analysis")
    @Auditable(action = AuditAction.DETAIL_VIEW)
    public ResponseEntity<List<LengthHistogramBucketDto>> getLengthHistogram() {
        var buckets = service.getLengthHistogram();
        return ResponseEntity.ok(buckets);
    }

    /**
     * Retrieves organism counts up to the specified limit.
     */
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

    /**
     * Retrieves the ratio of reviewed to unreviewed proteins.
     */
    @GetMapping("/reviewed-ratio")
    @RateLimited(key = "analysis")
    @Auditable(action = AuditAction.DETAIL_VIEW)
    public ResponseEntity<List<ReviewedRatioDto>> getReviewedRatio() {
        var ratios = service.getReviewedRatio();
        return ResponseEntity.ok(ratios);
    }

    /**
     * Retrieves evidence level distributions from experiments to predictions.
     */
    @GetMapping("/evidence-levels")
    @RateLimited(key = "analysis")
    @Auditable(action = AuditAction.DETAIL_VIEW)
    public ResponseEntity<List<EvidenceDistributionDto>> getEvidenceLevels() {
        var ev = service.getEvidenceLevels();
        return ResponseEntity.ok(ev);
    }

    /**
     * Retrieves most frequent keywords.
     */
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
