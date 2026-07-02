package com.bioinformatics.dashboard.gene.controller;

import com.bioinformatics.dashboard.audit.annotation.Auditable;
import com.bioinformatics.dashboard.audit.annotation.RateLimited;
import com.bioinformatics.dashboard.gene.service.GeneService;
import com.bioinformatics.dashboard.model.audit.AuditAction;
import com.bioinformatics.dashboard.model.gene.GeneSearchRequest;
import com.bioinformatics.dashboard.model.gene.PagedResponse;
import com.bioinformatics.dashboard.model.gene.ProteinDetailDto;
import com.bioinformatics.dashboard.model.gene.ProteinSummaryDto;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller providing APIs for retrieving and exporting gene/protein data.
 *
 * <p>Exposed endpoints include:</p>
 * <ul>
 *   <li>{@code GET  /api/genes}             — paginated basic list of genes</li>
 *   <li>{@code POST /api/genes/search}      — sophisticated querying via dynamic multi-filtering</li>
 *   <li>{@code GET  /api/genes/{id}}        — fetches deep, related details for a specific protein</li>
 *   <li>{@code GET /api/genes/export-csv}   — exports search results to CSV format</li>
 * </ul>
 *
 * <p>All endpoints enforce at least standard {@code USER} roles. Architecture dictates that this
 * controller remains thin, delegating complex Specification building and dataset projections to the
 * underlying {@link GeneService}. Check {@code documentation/api-contract.md} for strict contract schemas.</p>
 */
@RestController
@RequestMapping("/api/genes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','USER')")
public class GeneController {

    private final GeneService geneService;


    /**
     * GET /api/genes — paginated list with optional sort/direction.
     */
    @GetMapping
    @Auditable(action = AuditAction.SEARCH_QUERY)
    @RateLimited(key = "search")
    public ResponseEntity<PagedResponse<ProteinSummaryDto>> listGenes(
            @RequestParam(defaultValue = "0") int page,
            @Min(value = 1, message = "Page size should be greater than 0")
            @Max(value = 200, message = "Page size should be lower than 201")
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        var result = geneService.listGenes(page, size, sort, direction);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/genes/search — search and filter with full filter support.
     */
    @PostMapping("/search")
    @Auditable(action = AuditAction.SEARCH_QUERY)
    @RateLimited(key = "search")
    public ResponseEntity<PagedResponse<ProteinSummaryDto>> searchGenes(
            @RequestBody @Valid GeneSearchRequest request) {
        var result = geneService.searchGenes(request);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/genes/{id} — full protein detail.
     */
    @GetMapping("/{id}")
    @Auditable(action = AuditAction.DETAIL_VIEW, targetId = "#id")
    @RateLimited(key = "detail")
    public ResponseEntity<ProteinDetailDto> getGeneById(@PathVariable Long id) {
        return ResponseEntity.ok(geneService.getGeneById(id));
    }

    /**
     * POST /api/genes/export-csv — download CSV for filtered result set.
     */
    @PostMapping(value = "/export-csv", produces = "text/csv")
    @Auditable(action = AuditAction.DATA_EXPORT_CSV)
    @RateLimited(key = "export")
    public void exportCsv(
            @RequestBody @Valid GeneSearchRequest request,
            HttpServletResponse response) throws IOException {
        var totalRows = geneService.assertWithinExportLimit(request);
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        var filename = String.format("proteins_%s.csv", LocalDate.now());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(filename));
        try (var writer = response.getWriter()) {
            geneService.exportCsv(request, writer, totalRows);
        }
    }

    @GetMapping(value = "/keywords")
    @Auditable(action = AuditAction.SEARCH_QUERY)
    @RateLimited
    public List<String> loadKeywords() {
        return geneService.listKeywords();
    }
}
