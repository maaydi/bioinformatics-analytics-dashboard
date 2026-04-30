package com.bioinformatics.dashboard.gene.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bioinformatics.dashboard.gene.dto.GeneSearchRequest;
import com.bioinformatics.dashboard.gene.dto.PagedResponse;
import com.bioinformatics.dashboard.gene.dto.ProteinSummaryDto;
import com.bioinformatics.dashboard.gene.service.GeneService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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
public class GeneController {

    // TODO uncomment this if a concret class is created
    // private final GeneService geneService;

    /** GET /api/genes — paginated list with optional sort/direction. */
    @GetMapping
    public ResponseEntity<PagedResponse<ProteinSummaryDto>> listGenes(
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "50")   int size,
            @RequestParam(defaultValue = "id")   String sort,
            @RequestParam(defaultValue = "asc")  String direction) {
        // TODO: implement
        throw new UnsupportedOperationException("Not yet implemented — see plan.md");
    }

    /** POST /api/genes/search — search and filter with full filter support. */
    @PostMapping("/search")
    public ResponseEntity<PagedResponse<ProteinSummaryDto>> searchGenes(
            @RequestBody @Valid GeneSearchRequest request) {
        // TODO: implement
        throw new UnsupportedOperationException("Not yet implemented — see plan.md");
    }

    /** GET /api/genes/{id} — full protein detail. */
    @GetMapping("/{id}")
    public ResponseEntity<?> getGeneById(@PathVariable Long id) {
        // TODO: implement — return ProteinDetailDto
        throw new UnsupportedOperationException("Not yet implemented — see plan.md");
    }

    /** POST /api/genes/export-csv — download CSV for filtered result set. */
    @PostMapping("/export-csv")
    public void exportCsv(
            @RequestBody @Valid GeneSearchRequest request,
            HttpServletResponse response) {
        // TODO: implement — streams CSV into response
        throw new UnsupportedOperationException("Not yet implemented — see plan.md");
    }
}
