package com.bioinformatics.importservice.controller;

import com.bioinformatics.common.config.web.CurrentUser;
import com.bioinformatics.common.models.PagedResponse;
import com.bioinformatics.importservice.dto.ImportJobProgress;
import com.bioinformatics.importservice.dto.ImportJobSummary;
import com.bioinformatics.importservice.service.ImportService;
import com.bioinformatics.importservice.validator.ValidFileType;
import com.bioinformatics.shared.models.security.UserPrincipal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Controller for managing UniProt batch import operations.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Receive file uploads or remote filter selections
 *   <li>Validate requests and delegate to ImportService
 *   <li>Return job summaries and progress information
 * </ul>
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/v1/admin/import/uniprot - Trigger file-based import
 *   <li>POST /api/v1/admin/import/uniprot/remote - Trigger API-based import
 *   <li>GET /api/v1/admin/import/status - List all import jobs (paginated)
 *   <li>GET /api/v1/admin/import/status/{jobId} - Poll single job progress
 * </ul>
 *
 * <p>Access: Admin-only (requires ROLE_ADMIN).
 * Delegates orchestration of Spring Batch jobs to {@link ImportService}.
 */
@RestController
@RequestMapping("/api/v1/admin/import")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService service;

    /**
     * Triggers a file-based UniProt import job.
     *
     * <p>Accepts a UniProt data file (.dat or .tsv) and import strategy.
     * Enqueues async batch job and returns job summary immediately.
     *
     * @param file     uploaded file (.dat or .tsv)
     * @param strategy import strategy (OVERWRITE or APPEND)
     * @return ACCEPTED (202) with job summary
     */
    @PostMapping("/uniprot")
    public ResponseEntity<ImportJobSummary> triggerImport(
            @RequestParam("file") @ValidFileType MultipartFile file,
            @RequestParam("strategy") String strategy) {
        var job = service.triggerImport(file, strategy);
        return ResponseEntity.accepted().body(job);
    }

    /**
     * Triggers a remote UniProt API-based import job.
     *
     * <p>Fetches protein data from UniProt API using a saved filter.
     * Enqueues async batch job and returns job summary immediately.
     *
     * @param filterId    saved filter identifier
     * @param currentUser authenticated user making request
     * @return ACCEPTED (202) with job summary
     */
    @PostMapping("/uniprot/remote")
    public ResponseEntity<ImportJobSummary> triggerRemoteImport(
            @RequestParam("filterId") long filterId,
            @CurrentUser UserPrincipal currentUser) {
        var job = service.triggerRemoteImport(filterId, currentUser);
        return ResponseEntity.accepted().body(job);
    }

    /**
     * Lists all import jobs with pagination.
     *
     * @param page zero-indexed page number (default: 0)
     * @param size page size, max 200 (default: 20)
     * @return paginated list of import job summaries
     */
    @GetMapping("/status")
    public PagedResponse<ImportJobSummary> listImportJobs(
            @RequestParam(defaultValue = "0") int page,
            @Min(value = 1, message = "Page size should be greater than 0")
            @Max(value = 200, message = "Page size should be lower than 201")
            @RequestParam(defaultValue = "20") int size) {
        return service.listImportJobs(page, size);
    }

    /**
     * Polls current progress of a single import job.
     *
     * <p>Returns:
     * <ul>
     *   <li>Current status (RUNNING/COMPLETED/FAILED)
     *   <li>Records processed vs. estimated
     *   <li>Progress percentage
     *   <li>Timing information
     *   <li>Error message (if failed)
     * </ul>
     *
     * @param jobId import job UUID as string
     * @return job progress with metrics
     */
    @GetMapping("/status/{jobId}")
    public ImportJobProgress getImportJobStatus(@PathVariable String jobId) {
        return service.getImportJobStatus(jobId);
    }

}
