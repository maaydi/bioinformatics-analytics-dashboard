package com.bioinformatics.dashboard.analytics.controller;

import com.bioinformatics.dashboard.audit.annotation.Auditable;
import com.bioinformatics.dashboard.audit.annotation.RateLimited;
import com.bioinformatics.dashboard.audit.dto.AuditAction;
import com.bioinformatics.dashboard.interfaces.analytics.FilteredAnalyticsService;
import com.bioinformatics.dashboard.model.analytics.*;
import com.bioinformatics.dashboard.model.analytics.compare.CompareRequestDto;
import com.bioinformatics.dashboard.model.analytics.compare.CompareResponseDto;
import com.bioinformatics.dashboard.model.gene.GeneSearchRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller providing dynamic analytics endpoints supporting user-defined filters.
 *
 * <p>Unlike the static analytics controller which hits materialized views, this controller
 * relies on Spring Data JPA Specifications built dynamically from user requests, thereby
 * returning aggregated analytical data corresponding exactly to the provided query parameters.</p>
 *
 * <p>Delegates calculation and projection to the {@link FilteredAnalyticsService}.
 * Strict payload schemas follow {@code documentation/api-contract.md}.</p>
 */
@RestController
@Validated
@RequestMapping("/api/analytics/filters")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','USER')")
public class FilteredAnalyticsController {

    private final FilteredAnalyticsService service;

    /**
     * Calculates top-level KPIs for a filtered subset.
     */
    @PostMapping("/dashboard-kpis")
    @RateLimited(key = "analysis")
    @Auditable(action = AuditAction.DETAIL_VIEW)
    public ResponseEntity<DashboardKpisDto> getDashboardKpis(@RequestBody @Valid GeneSearchRequest request) {
        var kpis = service.getDashboardKpis(request);
        return ResponseEntity.ok(kpis);
    }

    /**
     * Calculates the length distribution histogram buckets for a filtered subset.
     */
    @PostMapping("/length-histogram")
    @RateLimited(key = "analysis")
    @Auditable(action = AuditAction.DETAIL_VIEW)
    public ResponseEntity<List<LengthHistogramBucketDto>> getLengthHistogram(@RequestBody @Valid GeneSearchRequest request) {
        var buckets = service.getLengthHistogram(request);
        return ResponseEntity.ok(buckets);
    }

    /**
     * Calculates top organism occurrences for a filtered subset.
     */
    @PostMapping("/by-organism")
    @RateLimited(key = "analysis")
    @Auditable(action = AuditAction.DETAIL_VIEW)
    public ResponseEntity<List<OrganismCountDto>> getByOrganism(
            @Min(value = 1, message = "Limit should be greater than 0")
            @Max(value = 200, message = "Limit should be lower than 201")
            @RequestParam(defaultValue = "50") int limit,
            @RequestBody @Valid GeneSearchRequest request) {
        var count = service.getByOrganism(limit, request);
        return ResponseEntity.ok(count);
    }

    /**
     * Calculates the ratio of reviewed to unreviewed proteins within the filtered subset.
     */
    @PostMapping("/reviewed-ratio")
    @RateLimited(key = "analysis")
    @Auditable(action = AuditAction.DETAIL_VIEW)
    public ResponseEntity<List<ReviewedRatioDto>> getReviewedRatio(@RequestBody @Valid GeneSearchRequest request) {
        var ratios = service.getReviewedRatio(request);
        return ResponseEntity.ok(ratios);
    }

    /**
     * Calculates the distribution of evidence levels within the filtered subset.
     */
    @PostMapping("/evidence-levels")
    @RateLimited(key = "analysis")
    @Auditable(action = AuditAction.DETAIL_VIEW)
    public ResponseEntity<List<EvidenceDistributionDto>> getEvidenceLevels(@RequestBody @Valid GeneSearchRequest request) {
        var ev = service.getEvidenceLevels(request);
        return ResponseEntity.ok(ev);
    }

    /**
     * Calculates the most frequent keywords for the filtered subset.
     */
    @PostMapping("/keyword-frequency")
    @RateLimited(key = "analysis")
    @Auditable(action = AuditAction.DETAIL_VIEW)
    public ResponseEntity<List<KeywordFrequencyDto>> getKeywordFrequency(
            @Min(value = 1, message = "Limit should be greater than 0")
            @Max(value = 500, message = "Limit should be lower than 501")
            @RequestParam(defaultValue = "100") int limit,
            @RequestBody @Valid GeneSearchRequest request) {
        var keywords = service.getKeywordFrequency(limit, request);
        return ResponseEntity.ok(keywords);
    }

    /**
     * Calculates raw protein length distribution, avoiding bucket scaling for granular analysis.
     */
    @PostMapping("/length-weight")
    @RateLimited(key = "analysis")
    @Auditable(action = AuditAction.DETAIL_VIEW)
    public ResponseEntity<List<ProteinLengthWeightCount>> getProteinLengthWeightCount(
            @RequestBody @Valid GeneSearchRequest request) {
        var raws = service.getProteinLengthWeightCount(request);
        return ResponseEntity.ok(raws);
    }

    /**
     * Compares analytics metrics for two distinct search requests side by side.
     * Useful for evaluating differences in metrics between separated groups.
     *
     * @param request encapsulates subsets A and B
     */
    @PostMapping("/compare")
    @RateLimited(key = "analysis")
    @Auditable(action = AuditAction.COMPARE_ANALYTICS)
    public ResponseEntity<CompareResponseDto> compare(@RequestBody @Valid CompareRequestDto request) {
        var result = service.compare(request);
        return ResponseEntity.ok(result);
    }
}
