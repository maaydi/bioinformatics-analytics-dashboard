package com.bioinformatics.dashboard.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bioinformatics.dashboard.admin.dto.ImportJobProgress;
import com.bioinformatics.dashboard.admin.dto.ImportJobSummary;
import com.bioinformatics.dashboard.admin.exception.UnsupportedFileTypeException;
import com.bioinformatics.dashboard.admin.service.ImportService;
import com.bioinformatics.dashboard.admin.validator.ValidFileSize;
import com.bioinformatics.dashboard.admin.validator.ValidFileType;
import com.bioinformatics.dashboard.gene.dto.PagedResponse;

import lombok.RequiredArgsConstructor;

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

    /** POST /api/admin/import/uniprot — triggers Spring Batch import job. */
    @PostMapping("/uniprot")
    public ResponseEntity<ImportJobSummary> triggerImport(
            @RequestParam("file") @ValidFileType @ValidFileSize MultipartFile file,
            @RequestParam("strategy") String strategy) {
        var job = service.triggertImport(file, strategy);
        return ResponseEntity.accepted().body(job);
    }

    /** GET /api/admin/import/status — paginated list of all import jobs. */
    @GetMapping("/status")
    public PagedResponse<ImportJobSummary> listImportJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listImportJobs(page, size);
    }

    /**
     * GET /api/admin/import/status/{jobId} — real-time progress of a single job.
     */
    @GetMapping("/status/{jobId}")
    public ImportJobProgress getImportJobStatus(@PathVariable String jobId) {
        return service.getImportJobStatus(jobId);
    }

    @ExceptionHandler(UnsupportedFileTypeException.class)
    public ResponseEntity<Object> handleUnsupportedFileTypeException(UnsupportedFileTypeException ex) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(ex.getMessage());
    }
}
