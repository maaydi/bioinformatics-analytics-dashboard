package com.bioinformatics.dashboard.admin.controller;

import com.bioinformatics.dashboard.admin.service.ImportService;
import com.bioinformatics.dashboard.admin.validator.ValidFileType;
import com.bioinformatics.dashboard.audit.annotation.Auditable;
import com.bioinformatics.dashboard.audit.annotation.RateLimited;
import com.bioinformatics.dashboard.audit.dto.AuditAction;
import com.bioinformatics.dashboard.job.dto.ImportJobProgress;
import com.bioinformatics.dashboard.job.dto.ImportJobSummary;
import com.bioinformatics.dashboard.model.gene.PagedResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Controller for managing UniProt batch import operations.
 * <p>
 * Exposes endpoints to trigger large-scale data imports and monitor job status.
 * Access is restricted to users with the ROLE_ADMIN authority.
 * Delegates orchestration of Spring Batch jobs to the {@link ImportService}.
 * Consult {@code documentation/api-contract.md} for detailed endpoint specifications.
 * </p>
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
    @Auditable(action = AuditAction.IMPORT_UPLOAD, targetId = "#result.id")
    @RateLimited(key = "import")
    public ResponseEntity<ImportJobSummary> triggerImport(
            @RequestParam("file") @ValidFileType MultipartFile file,
            @RequestParam("strategy") String strategy) {
        var job = service.triggerImport(file, strategy);
        return ResponseEntity.accepted().body(job);
    }

    /**
     * POST /api/admin/import/uniprot/remote — triggers remote UniProt API import job.
     */
    @PostMapping("/uniprot/remote")
    @Auditable(action = AuditAction.IMPORT_UPLOAD, targetId = "#result.id")
    @RateLimited(key = "import")
    public ResponseEntity<ImportJobSummary> triggerRemoteImport(@RequestParam("filterId") long filterId) {
        var job = service.triggerRemoteImport(filterId);
        return ResponseEntity.accepted().body(job);
    }

    /**
     * GET /api/admin/import/status — paginated list of all import jobs.
     */
    @GetMapping("/status")
    @Auditable(action = AuditAction.IMPORT_UPLOAD)
    @RateLimited
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
    @Auditable(action = AuditAction.IMPORT_UPLOAD, targetId = "#jobId")
    @RateLimited
    public ImportJobProgress getImportJobStatus(@PathVariable String jobId) {
        return service.getImportJobStatus(jobId);
    }

}
