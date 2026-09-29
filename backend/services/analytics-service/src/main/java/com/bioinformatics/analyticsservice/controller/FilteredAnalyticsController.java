package com.bioinformatics.analyticsservice.controller;


import com.bioinformatics.analyticsservice.interfaces.FilteredAnalyticsService;
import com.bioinformatics.analyticsservice.models.*;
import com.bioinformatics.analyticsservice.models.compare.CompareRequestDto;
import com.bioinformatics.analyticsservice.models.compare.CompareResponseDto;
import com.bioinformatics.common.models.gene.GeneSearchRequest;
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
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/v1/analytics/filters/dashboard-kpis - KPIs for filtered subset
 *   <li>POST /api/v1/analytics/filters/length-histogram - Length distribution for subset
 *   <li>POST /api/v1/analytics/filters/by-organism - Top organisms in subset
 *   <li>POST /api/v1/analytics/filters/reviewed-ratio - Reviewed ratio in subset
 *   <li>POST /api/v1/analytics/filters/evidence-levels - Evidence distribution in subset
 *   <li>POST /api/v1/analytics/filters/keyword-frequency - Top keywords in subset
 *   <li>POST /api/v1/analytics/filters/length-weight - Raw length/weight pairs in subset
 *   <li>POST /api/v1/analytics/filters/compare - Side-by-side comparison of two subsets
 * </ul>
 *
 * <p>All methods accept a GeneSearchRequest (filter criteria) as POST body.
 * Response times typically 100-2000ms depending on filter selectivity.
 * Access: USER or ADMIN roles required.
 */
@RestController
@Validated
@RequestMapping("/api/v1/analytics/filters")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','USER')")
public class FilteredAnalyticsController {

    private final FilteredAnalyticsService service;

    /**
     * Calculates top-level KPIs for a filtered subset.
     *
     * <p>Dynamic calculation based on provided filter criteria.
     * No caching; results vary per filter.
     *
     * @param request gene search filter (organism, keyword, length range, etc.)
     * @return OK (200) with filtered KPIs
     */
    @PostMapping("/dashboard-kpis")
    public ResponseEntity<DashboardKpisDto> getDashboardKpis(@RequestBody @Valid GeneSearchRequest request) {
        var kpis = service.getDashboardKpis(request);
        return ResponseEntity.ok(kpis);
    }

    /**
     * Calculates the length distribution histogram buckets for a filtered subset.
     *
     * <p>Dynamically buckets protein lengths based on filtered data.
     * May return different bucket ranges than static histogram.
     *
     * @param request gene search filter
     * @return OK (200) with filtered histogram buckets
     */
    @PostMapping("/length-histogram")
    public ResponseEntity<List<LengthHistogramBucketDto>> getLengthHistogram(@RequestBody @Valid GeneSearchRequest request) {
        var buckets = service.getLengthHistogram(request);
        return ResponseEntity.ok(buckets);
    }

    /**
     * Calculates top organism occurrences for a filtered subset.
     *
     * @param limit maximum results (default: 50, max: 200)
     * @param request gene search filter
     * @return OK (200) with filtered organism counts
     */
    @PostMapping("/by-organism")
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
     *
     * @param request gene search filter
     * @return OK (200) with filtered reviewed/unreviewed ratios
     */
    @PostMapping("/reviewed-ratio")
    public ResponseEntity<List<ReviewedRatioDto>> getReviewedRatio(@RequestBody @Valid GeneSearchRequest request) {
        var ratios = service.getReviewedRatio(request);
        return ResponseEntity.ok(ratios);
    }

    /**
     * Calculates the distribution of evidence levels within the filtered subset.
     *
     * @param request gene search filter
     * @return OK (200) with filtered evidence distribution
     */
    @PostMapping("/evidence-levels")
    public ResponseEntity<List<EvidenceDistributionDto>> getEvidenceLevels(@RequestBody @Valid GeneSearchRequest request) {
        var ev = service.getEvidenceLevels(request);
        return ResponseEntity.ok(ev);
    }

    /**
     * Calculates the most frequent keywords for the filtered subset.
     *
     * @param limit maximum keywords (default: 100, max: 500)
     * @param request gene search filter
     * @return OK (200) with filtered keyword frequencies
     */
    @PostMapping("/keyword-frequency")
    public ResponseEntity<List<KeywordFrequencyDto>> getKeywordFrequency(
            @Min(value = 1, message = "Limit should be greater than 0")
            @Max(value = 500, message = "Limit should be lower than 501")
            @RequestParam(defaultValue = "100") int limit,
            @RequestBody @Valid GeneSearchRequest request) {
        var keywords = service.getKeywordFrequency(limit, request);
        return ResponseEntity.ok(keywords);
    }

    /**
     * Calculates raw protein length/weight counts for granular analysis.
     *
     * <p>Unlike bucketed histogram, returns individual (length, weight, count) tuples.
     * Useful for fine-grained UI rendering (scatter plots, etc.).
     *
     * @param request gene search filter
     * @return OK (200) with raw length/weight pairs
     */
    @PostMapping("/length-weight")
    public ResponseEntity<List<ProteinLengthWeightCount>> getProteinLengthWeightCount(
            @RequestBody @Valid GeneSearchRequest request) {
        var raws = service.getProteinLengthWeightCount(request);
        return ResponseEntity.ok(raws);
    }

    /**
     * Compares analytics metrics for two distinct search requests side by side.
     *
     * <p>Useful for evaluating differences between separated groups:
     * <ul>
     *   <li>Pathway A vs Pathway B
     *   <li>Species 1 vs Species 2
     *   <li>New discoveries vs Reviewed entries
     * </ul>
     *
     * <p>Request contains subsets A and B with independent filters.
     * Response provides metrics for each subset.
     *
     * @param request comparison specification (subsetA and subsetB)
     * @return OK (200) with comparison results
     */
    @PostMapping("/compare")
    public ResponseEntity<CompareResponseDto> compare(@RequestBody @Valid CompareRequestDto request) {
        var result = service.compare(request);
        return ResponseEntity.ok(result);
    }
}
