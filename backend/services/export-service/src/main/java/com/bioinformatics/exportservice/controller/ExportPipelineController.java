package com.bioinformatics.exportservice.controller;

import com.bioinformatics.common.config.web.CurrentUser;
import com.bioinformatics.common.models.PagedResponse;
import com.bioinformatics.exportservice.dto.*;
import com.bioinformatics.exportservice.service.ExportPipelineService;
import com.bioinformatics.shared.models.gene.ExportFieldSchema;
import com.bioinformatics.shared.models.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * REST controller for export pipeline management.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/v1/exports/pipelines - Create and enqueue export
 *   <li>GET /api/v1/exports/pipelines - List user's pipelines (with status filter)
 *   <li>GET /api/v1/exports/pipelines/{id} - Get pipeline details
 *   <li>GET /api/v1/exports/pipelines/{id}/status - Poll job progress
 *   <li>GET /api/v1/exports/pipelines/{id}/download - Get download URL (once complete)
 *   <li>GET /api/v1/exports/pipelines/{id}/download-file - Download file
 *   <li>POST /api/v1/exports/pipelines/{id}/retry - Retry failed export
 *   <li>DELETE /api/v1/exports/pipelines/{id} - Soft-delete pipeline
 *   <li>GET /api/v1/exports/fields - List available export fields
 * </ul>
 *
 * <p>All endpoints require ADMIN or USER role.
 *
 * @see ExportPipelineService
 */
@RestController
@Validated
@RequestMapping("/api/v1/exports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','USER')")
public class ExportPipelineController {

    private final ExportPipelineService exportPipelineService;

    /**
     * Creates a new export pipeline and enqueues it for async processing.
     *
     * @param request export configuration (filter, format, fields)
     * @param user    authenticated user
     * @return CREATED (201) with pipeline DTO
     */
    @PostMapping("/pipelines")
    public ResponseEntity<ExportPipelineResponse> createPipeline(@Valid @RequestBody ExportPipelineCreateRequest request,
                                                                 @CurrentUser UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exportPipelineService.createPipeline(request, user));
    }

    /**
     * Lists pipelines for the authenticated user with optional status filtering.
     *
     * @param status optional status filter (QUEUED/RUNNING/COMPLETED/FAILED/CANCELLED)
     * @param page   zero-indexed page number (default: 0)
     * @param size   page size, max 50 (default: 20)
     * @param user   authenticated user
     * @return OK (200) with paginated pipeline list
     */
    @GetMapping("/pipelines")
    public ResponseEntity<PagedResponse<ExportPipelineResponse>> listPipelines(
            @RequestParam(required = false) ExportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @Min(value = 1, message = "Page size should be greater than 0")
            @Max(value = 50, message = "Page size should be lower than 51")
            @RequestParam(defaultValue = "20") int size,
            @CurrentUser UserPrincipal user) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(exportPipelineService.listPipelines(status, pageable, user));
    }

    /**
     * Retrieves a single pipeline by ID.
     *
     * @param id   pipeline identifier
     * @param user authenticated user
     * @return OK (200) with pipeline DTO
     */
    @GetMapping("/pipelines/{id}")
    public ResponseEntity<ExportPipelineResponse> getPipeline(
            @PathVariable("id") long id,
            @CurrentUser UserPrincipal user) {
        return ResponseEntity.ok(exportPipelineService.getPipeline(id, user));
    }

    /**
     * Polls current job status and progress.
     *
     * <p>Returns progress metrics for UI progress bar:
     * <ul>
     *   <li>Current status (QUEUED/RUNNING/COMPLETED/FAILED)
     *   <li>Progress percentage (0–100)
     *   <li>Chunks processed vs. total
     *   <li>Current batch step name
     * </ul>
     *
     * @param id   pipeline identifier
     * @param user authenticated user
     * @return OK (200) with status and progress
     */
    @GetMapping("/pipelines/{id}/status")
    public ResponseEntity<ExportJobStatusResponse> getPipelineJobStatus(
            @PathVariable("id") long id,
            @CurrentUser UserPrincipal user) {
        return ResponseEntity.ok(exportPipelineService.getPipelineStatus(id, user));
    }

    /**
     * Requests download URL for a completed export.
     *
     * <p>Only available after pipeline status is COMPLETED.
     *
     * @param id   pipeline identifier
     * @param user authenticated user
     * @return OK (200) with download URL and file metadata
     */
    @GetMapping("/pipelines/{id}/download")
    public ResponseEntity<DownloadUrlDto> getPipelineDownloadUrl(
            @PathVariable("id") long id,
            @CurrentUser UserPrincipal user) throws IOException {
        return ResponseEntity.ok(exportPipelineService.getDownloadUrl(id, user));
    }

    /**
     * Downloads the export file for a completed pipeline.
     *
     * <p>Returns file with correct MIME type and Content-Disposition header.
     *
     * @param id   pipeline identifier
     * @param user authenticated user
     * @return OK (200) with file as binary attachment
     * @throws IOException if file cannot be read
     */
    @GetMapping("/pipelines/{id}/download-file")
    public ResponseEntity<Resource> downloadFile(@PathVariable("id") long id, @CurrentUser UserPrincipal user) throws IOException {
        var fileStream = exportPipelineService.getExportFileStream(id, user);
        var formatWriter = fileStream.formatWriter();
        var resource = new InputStreamResource(fileStream.stream());
        var contentDisposition = ContentDisposition.attachment()
                .filename(fileStream.filename(), StandardCharsets.UTF_8)
                .build();
        var headers = new HttpHeaders();
        headers.setContentDisposition(contentDisposition);
        if (fileStream.filesize() > 0) {
            headers.setContentLength(fileStream.filesize());
        }
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(formatWriter.getFormat().getContentType()))
                .body(resource);
    }

    /**
     * Retries a failed or cancelled export by cloning its configuration.
     *
     * <p>Only available if pipeline is in terminal state (FAILED/CANCELLED).
     *
     * @param id   pipeline identifier
     * @param user authenticated user
     * @return ACCEPTED (202) with new pipeline DTO
     */
    @PostMapping("/pipelines/{id}/retry")
    public ResponseEntity<ExportPipelineResponse> retry(@PathVariable("id") long id, @CurrentUser UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(exportPipelineService.retryPipeline(id, user));
    }

    /**
     * Soft-deletes a pipeline.
     *
     * <p>If running, stops the batch job before deletion.
     * Physical cleanup occurs after retention period (default 30 days).
     *
     * @param id   pipeline identifier
     * @param user authenticated user
     * @return NO_CONTENT (204)
     */
    @DeleteMapping("/pipelines/{id}")
    public ResponseEntity<Void> deletePipeline(@PathVariable("id") long id, @CurrentUser UserPrincipal user) {
        exportPipelineService.deletePipeline(id, user);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Lists all available export fields for protein detail DTO.
     *
     * <p>Used by UI to populate field selection dialog.
     *
     * @return OK (200) with array of exportable fields
     */
    @GetMapping("/fields")
    public ResponseEntity<List<ExportFieldSchema>> getAvailableFields() {
        return ResponseEntity.ok(exportPipelineService.getAvailableFields());
    }

}
