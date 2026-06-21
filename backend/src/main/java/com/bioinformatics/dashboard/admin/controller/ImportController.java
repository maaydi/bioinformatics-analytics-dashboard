package com.bioinformatics.dashboard.admin.controller;

import com.bioinformatics.dashboard.admin.service.ImportService;
import com.bioinformatics.dashboard.admin.validator.ValidFileType;
import com.bioinformatics.dashboard.audit.annotation.Auditable;
import com.bioinformatics.dashboard.audit.dto.AuditAction;
import com.bioinformatics.dashboard.gene.dto.PagedResponse;
import com.bioinformatics.dashboard.job.dto.ImportJobProgress;
import com.bioinformatics.dashboard.job.dto.ImportJobSummary;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for UniProt import administration.
 *
 * <p>
 * All endpoints are restricted to {@code ROLE_ADMIN}.
 * Contract: documentation/api-contract.md §3 — Import Admin Endpoints.
 *
 * <ul>
 * <li>{@code POST /api/admin/import/uniprot} — trigger import job</li>
 * <li>{@code GET  /api/admin/import/status} — list all jobs</li>
 * <li>{@code GET  /api/admin/import/status/{jobId}} — poll single job</li>
 * </ul>
 *
 * <p>
 * File upload limits:
 * <ul>
 * <li>Max size: 2 GB (enforced by Spring multipart +
 * GlobalExceptionHandler)</li>
 * <li>Accepted types: .dat, .tsv (validated in batch ItemProcessor)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/import")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService service;

    /**
     * POST /api/admin/import/uniprot — triggers Spring Batch import job.
     */
    @PostMapping("/uniprot")
    @Auditable(action = AuditAction.IMPORT_UPLOAD)
    public ResponseEntity<ImportJobSummary> triggerImport(
            @RequestParam("file") @ValidFileType MultipartFile file,
            @RequestParam("strategy") String strategy) {
        var job = service.triggerImport(file, strategy);
        return ResponseEntity.accepted().body(job);
    }

    /**
     * GET /api/admin/import/status — paginated list of all import jobs.
     */
    @GetMapping("/status")
    @Auditable(action = AuditAction.IMPORT_UPLOAD)
    public PagedResponse<ImportJobSummary> listImportJobs(
            @RequestParam(defaultValue = "0") int page,
            @Min(value = 1, message = "Page size should be greater than 0")
            @Max(value = 200, message = "Page size should be lower than 201")
            @RequestParam(defaultValue = "20") int size) {
        return service.listImportJobs(page, size);
    }

    /**
     * GET /api/admin/import/status/{jobId} — real-time progress of a single job.
     */
    @GetMapping("/status/{jobId}")
    @Auditable(action = AuditAction.IMPORT_UPLOAD)
    public ImportJobProgress getImportJobStatus(@PathVariable String jobId) {
        return service.getImportJobStatus(jobId);
    }

    // TODO add cancel import


}
