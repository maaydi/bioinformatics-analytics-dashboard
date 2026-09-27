package com.bioinformatics.dashboard.gene.controller;

import com.bioinformatics.common.gene.dto.ProteinDetailDto;
import com.bioinformatics.common.gene.dto.ProteinSummaryDto;
import com.bioinformatics.common.models.PagedResponse;
import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.dashboard.audit.annotation.Auditable;
import com.bioinformatics.dashboard.audit.annotation.RateLimited;
import com.bioinformatics.dashboard.audit.dto.AuditAction;
import com.bioinformatics.dashboard.interfaces.gene.GeneService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * REST controller exposing gene and protein discovery operations.
 *
 * <p>This controller is intentionally thin and delegates all domain-specific search, filtering, and retrieval logic
 * to {@link GeneService}. The service layer resolves the active backend provider and returns DTOs rather than JPA
 * entities, keeping the API contract stable across PostgreSQL and external UniProt-backed implementations.</p>
 */
@RestController
@RequestMapping("/api/genes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','USER')")
@Slf4j
public class GeneController {

    private final GeneService geneService;

    /**
     * Lists the available protein summaries with optional pagination and sorting.
     *
     * @param page zero-based pagination index
     * @param size page size, bounded to 1..200
     * @param sort field name to sort by
     * @param direction sort direction: asc or desc
     * @return paginated list of summary records
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
        log.info("[DASHBOARD][GENE] Listing genes page={} size={} sort={} direction={}", page, size, sort, direction);
        var result = geneService.listGenes(page, size, sort, direction);
        return ResponseEntity.ok(result);
    }

    /**
     * Executes a filtered protein search using a structured {@link GeneSearchRequest} payload.
     *
     * @param request search and pagination criteria
     * @return paginated protein summary set matching the request
     */
    @PostMapping("/search")
    @Auditable(action = AuditAction.SEARCH_QUERY)
    @RateLimited(key = "search")
    public ResponseEntity<PagedResponse<ProteinSummaryDto>> searchGenes(
            @RequestBody @Valid GeneSearchRequest request) {
        log.info("[DASHBOARD][GENE] Searching genes with request={}", request);
        var result = geneService.searchGenes(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves the full detail for a single gene by accession.
     *
     * @param accession protein accession identifier
     * @return full detail payload for the requested protein
     */
    @GetMapping("/{accession}")
    @Auditable(action = AuditAction.DETAIL_VIEW, targetId = "#accession")
    @RateLimited(key = "detail")
    public ResponseEntity<ProteinDetailDto> getGeneById(@PathVariable String accession) {
        log.info("[DASHBOARD][GENE] Loading gene detail accession={}", accession);
        return ResponseEntity.ok(geneService.getGeneByAccession(accession));
    }

    /**
     * Exports the current search result set to CSV.
     *
     * @param request export criteria
     * @param response HTTP response used to stream the file
     * @throws IOException when writing the CSV document fails
     */
    @PostMapping(value = "/export-csv", produces = "text/csv")
    @Auditable(action = AuditAction.DATA_EXPORT_CSV)
    @RateLimited(key = "export")
    public void exportCsv(
            @RequestBody @Valid GeneSearchRequest request,
            HttpServletResponse response) throws IOException {
        log.warn("[DASHBOARD][GENE] CSV export requested but not implemented for request={}", request);
        throw new RuntimeException("Not implemented");
    }

    /**
     * Counts the number of rows matching a gene search request.
     *
     * @param request filter request used for counting
     * @return number of matching records
     */
    @PostMapping(value = "/count")
    @Auditable(action = AuditAction.DATA_EXPORT_CSV)
    @RateLimited(key = "export")
    public long countRequestRows(
            @RequestBody @Valid GeneSearchRequest request) {
        log.info("[DASHBOARD][GENE] Counting genes matching request={}", request);
        return geneService.count(request);
    }
}
