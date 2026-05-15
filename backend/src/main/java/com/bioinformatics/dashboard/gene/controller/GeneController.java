package com.bioinformatics.dashboard.gene.controller;

import com.bioinformatics.dashboard.gene.dto.GeneSearchRequest;
import com.bioinformatics.dashboard.gene.dto.PagedResponse;
import com.bioinformatics.dashboard.gene.dto.ProteinDetailDto;
import com.bioinformatics.dashboard.gene.dto.ProteinSummaryDto;
import com.bioinformatics.dashboard.gene.service.GeneService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * REST controller for gene/protein endpoints.
 *
 * <p>Contract: documentation/api-contract.md §1 — Gene / Protein Endpoints.
 * <ul>
 *   <li>{@code GET  /api/genes}             — paginated list</li>
 *   <li>{@code POST /api/genes/search}       — search + multi-filter</li>
 *   <li>{@code GET  /api/genes/{id}}         — full protein detail</li>
 *   <li>{@code POST /api/genes/export-csv}   — CSV export</li>
 * </ul>
 *
 * <p>Authorization: USER and ADMIN (see SecurityConfig).
 * Controllers are intentionally thin — all business logic lives in {@link GeneService}.
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
    public ResponseEntity<PagedResponse<ProteinSummaryDto>> listGenes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        var direct = Sort.Direction.fromString(direction);
        var result = geneService.listGenes(PageRequest.of(page, size, direct, sort));
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/genes/search — search and filter with full filter support.
     */
    @PostMapping("/search")
    public ResponseEntity<PagedResponse<ProteinSummaryDto>> searchGenes(
            @RequestBody @Valid GeneSearchRequest request) {
        var result = geneService.searchGenes(request);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/genes/{id} — full protein detail.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProteinDetailDto> getGeneById(@PathVariable Long id) {
        return ResponseEntity.ok(geneService.getGeneById(id));
    }

    /**
     * POST /api/genes/export-csv — download CSV for filtered result set.
     */
    @PostMapping(value = "/export-csv", produces = "text/csv")
    public void exportCsv(
            @RequestBody @Valid GeneSearchRequest request,
            HttpServletResponse response) throws IOException {
        // 2. Set headers
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"export.csv\"");
        try (var writer = response.getWriter()) {
            geneService.exportCsv(request, writer);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to export csv " + e.getMessage());
        }
    }
}
