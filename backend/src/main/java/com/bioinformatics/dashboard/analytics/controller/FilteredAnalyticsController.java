package com.bioinformatics.dashboard.analytics.controller;

import com.bioinformatics.dashboard.analytics.dto.*;
import com.bioinformatics.dashboard.analytics.dto.compare.CompareRequestDto;
import com.bioinformatics.dashboard.analytics.dto.compare.CompareResponseDto;
import com.bioinformatics.dashboard.analytics.service.FilteredAnalyticsService;
import com.bioinformatics.dashboard.gene.dto.GeneSearchRequest;
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
 * REST controller for analytics chart endpoints.
 *
 * <p>All endpoints are served from repository based on GeneSearchRequest.
 * See documentation/api-contract.md §2 — Analytics Endpoints.
 *
 * <ul>
 *   <li>{@code GET /api/analytics/dashboard-kpis}       </li>
 *   <li>{@code GET /api/analytics/length-histogram}     </li>
 *   <li>{@code GET /api/analytics/by-organism}         </li>
 *   <li>{@code GET /api/analytics/reviewed-ratio}       </li>
 *   <li>{@code GET /api/analytics/evidence-levels}      </li>
 *   <li>{@code GET /api/analytics/keyword-frequency}    </li>
 * </ul>
 *
 * <p>Authorization: USER and ADMIN.
 * Response time target: ≤ 500 ms (NFR §12.1).
 */
@RestController
@Validated
@RequestMapping("/api/analytics/filters")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','USER')")
public class FilteredAnalyticsController {

    private final FilteredAnalyticsService service;

    @PostMapping("/dashboard-kpis")
    public ResponseEntity<DashboardKpisDto> getDashboardKpis(@RequestBody @Valid GeneSearchRequest request) {
        var kpis = service.getDashboardKpis(request);
        return ResponseEntity.ok(kpis);
    }

    @PostMapping("/length-histogram")
    public ResponseEntity<List<LengthHistogramBucketDto>> getLengthHistogram(@RequestBody @Valid GeneSearchRequest request) {
        var buckets = service.getLengthHistogram(request);
        return ResponseEntity.ok(buckets);
    }

    @PostMapping("/by-organism")
    public ResponseEntity<List<OrganismCountDto>> getByOrganism(
            @Min(value = 1, message = "Limit should be greater than 0")
            @Max(value = 200, message = "Limit should be lower than 201")
            @RequestParam(defaultValue = "50") int limit,
            @RequestBody @Valid GeneSearchRequest request) {
        var count = service.getByOrganism(limit, request);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/reviewed-ratio")
    public ResponseEntity<List<ReviewedRatioDto>> getReviewedRatio(@RequestBody @Valid GeneSearchRequest request) {
        var ratios = service.getReviewedRatio(request);
        return ResponseEntity.ok(ratios);
    }

    @PostMapping("/evidence-levels")
    public ResponseEntity<List<EvidenceDistributionDto>> getEvidenceLevels(@RequestBody @Valid GeneSearchRequest request) {
        var ev = service.getEvidenceLevels(request);
        return ResponseEntity.ok(ev);
    }

    @PostMapping("/keyword-frequency")
    public ResponseEntity<List<KeywordFrequencyDto>> getKeywordFrequency(
            @Min(value = 1, message = "Limit should be greater than 0")
            @Max(value = 500, message = "Limit should be lower than 501")
            @RequestParam(defaultValue = "100") int limit,
            @RequestBody @Valid GeneSearchRequest request) {
        var keywords = service.getKeywordFrequency(limit, request);
        return ResponseEntity.ok(keywords);
    }

    @PostMapping("/length-weight")
    public ResponseEntity<List<ProteinLengthWeightCount>> getProteinLengthWeightCount(
            @RequestBody @Valid GeneSearchRequest request) {
        var raws = service.getProteinLengthWeightCount(request);
        return ResponseEntity.ok(raws);
    }

    @PostMapping("/compare")
    public ResponseEntity<CompareResponseDto> compare(@RequestBody @Valid CompareRequestDto request) {
        var result = service.compare(request);
        return ResponseEntity.ok(result);
    }
}
