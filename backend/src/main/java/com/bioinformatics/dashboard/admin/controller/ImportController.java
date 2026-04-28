package com.bioinformatics.dashboard.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for UniProt import administration.
 *
 * <p>All endpoints are restricted to {@code ROLE_ADMIN}.
 * Contract: documentation/api-contract.md §3 — Import Admin Endpoints.
 *
 * <ul>
 *   <li>{@code POST /api/admin/import/uniprot}            — trigger import job</li>
 *   <li>{@code GET  /api/admin/import/status}             — list all jobs</li>
 *   <li>{@code GET  /api/admin/import/status/{jobId}}     — poll single job</li>
 * </ul>
 *
 * <p>File upload limits:
 * <ul>
 *   <li>Max size: 2 GB (enforced by Spring multipart + GlobalExceptionHandler)</li>
 *   <li>Accepted types: .dat, .tsv (validated in batch ItemProcessor)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/import")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ImportController {

    // TODO: inject ImportService

    /** POST /api/admin/import/uniprot — triggers Spring Batch import job. */
    @PostMapping("/uniprot")
    public ResponseEntity<?> triggerImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam("strategy") String strategy) {
        // TODO: implement — returns 202 Accepted with ImportJobSummary
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /** GET /api/admin/import/status — paginated list of all import jobs. */
    @GetMapping("/status")
    public ResponseEntity<?> listImportJobs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /** GET /api/admin/import/status/{jobId} — real-time progress of a single job. */
    @GetMapping("/status/{jobId}")
    public ResponseEntity<?> getImportJobStatus(@PathVariable String jobId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
