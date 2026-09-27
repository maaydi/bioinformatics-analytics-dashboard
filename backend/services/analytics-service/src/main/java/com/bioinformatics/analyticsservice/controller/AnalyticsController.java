package com.bioinformatics.analyticsservice.controller;

import com.bioinformatics.analyticsservice.interfaces.AnalyticsService;
import com.bioinformatics.analyticsservice.models.*;
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
 * <p>Endpoints:
 * <ul>
 *   <li>GET /api/v1/analytics/dashboard-kpis - Overall statistics
 *   <li>GET /api/v1/analytics/length-histogram - Protein length distribution
 *   <li>GET /api/v1/analytics/by-organism - Top organisms
 *   <li>GET /api/v1/analytics/reviewed-ratio - Reviewed vs unreviewed
 *   <li>GET /api/v1/analytics/evidence-levels - Evidence distribution
 *   <li>GET /api/v1/analytics/keyword-frequency - Top keywords
 * </ul>
 *
 * <p>Results are cached in Redis for sub-100ms response times.
 * Access: USER or ADMIN roles required.
 */
@RestController
@Validated
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','USER')")
public class AnalyticsController {

    private final AnalyticsService service;

    /**
     * Retrieves top-level dashboard KPIs (total proteins, reviewed, etc.).
     *
     * <p>Serves data from materialized view cache or Redis.
     * Typical response time: 50-200ms.
     *
     * @return OK (200) with dashboard KPIs
     */
    @GetMapping("/dashboard-kpis")
    public ResponseEntity<DashboardKpisDto> getDashboardKpis() {
        var kpis = service.getDashboardKpis();
        return ResponseEntity.ok(kpis);
    }

    /**
     * Retrieves the length distribution histogram buckets.
     *
     * <p>Pre-computed bucketed distribution for chart rendering.
     * Typical response time: 50-150ms.
     *
     * @return OK (200) with histogram buckets
     */
    @GetMapping("/length-histogram")
    public ResponseEntity<List<LengthHistogramBucketDto>> getLengthHistogram() {
        var buckets = service.getLengthHistogram();
        return ResponseEntity.ok(buckets);
    }

    /**
     * Retrieves top organism occurrences with configurable limit.
     *
     * @param limit maximum results (default: 50, max: 200)
     * @return OK (200) with organism counts
     */
    @GetMapping("/by-organism")
    public ResponseEntity<List<OrganismCountDto>> getByOrganism(
            @Min(value = 1, message = "Limit should be greater than 0")
            @Max(value = 200, message = "Limit should be lower than 201")
            @RequestParam(defaultValue = "50") int limit) {
        var count = service.getByOrganism(limit);
        return ResponseEntity.ok(count);
    }

    /**
     * Retrieves the ratio of reviewed (UniProt reviewed) to unreviewed proteins.
     *
     * @return OK (200) with reviewed/unreviewed ratios
     */
    @GetMapping("/reviewed-ratio")
    public ResponseEntity<List<ReviewedRatioDto>> getReviewedRatio() {
        var ratios = service.getReviewedRatio();
        return ResponseEntity.ok(ratios);
    }

    /**
     * Retrieves evidence level distributions (experimental to predicted).
     *
     * @return OK (200) with evidence distributions
     */
    @GetMapping("/evidence-levels")
    public ResponseEntity<List<EvidenceDistributionDto>> getEvidenceLevels() {
        var ev = service.getEvidenceLevels();
        return ResponseEntity.ok(ev);
    }

    /**
     * Retrieves most frequent keywords with configurable limit.
     *
     * @param limit maximum keywords to return (default: 100, max: 500)
     * @return OK (200) with keyword frequency list
     */
    @GetMapping("/keyword-frequency")
    public ResponseEntity<List<KeywordFrequencyDto>> getKeywordFrequency(
            @Min(value = 1, message = "Limit should be greater than 0")
            @Max(value = 500, message = "Limit should be lower than 501")
            @RequestParam(defaultValue = "100") int limit) {
        var keywords = service.getKeywordFrequency(limit);
        return ResponseEntity.ok(keywords);
    }
}
