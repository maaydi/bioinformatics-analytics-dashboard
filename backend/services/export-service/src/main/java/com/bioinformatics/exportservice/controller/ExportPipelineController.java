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

@RestController
@Validated
@RequestMapping("/api/v1/exports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','USER')")
public class ExportPipelineController {

    private final ExportPipelineService exportPipelineService;

    @PostMapping("/pipelines")
    public ResponseEntity<ExportPipelineResponse> createPipeline(@Valid @RequestBody ExportPipelineCreateRequest request,
                                                                 @CurrentUser UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exportPipelineService.createPipeline(request, user));
    }

    @GetMapping("/pipelines")
    public ResponseEntity<PagedResponse<ExportPipelineResponse>> createPipeline(
            @RequestParam(required = false) ExportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @Min(value = 1, message = "Page size should be greater than 0")
            @Max(value = 50, message = "Page size should be lower than 51")
            @RequestParam(defaultValue = "20") int size,
            @CurrentUser UserPrincipal user) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(exportPipelineService.listPipelines(status, pageable, user));
    }

    @GetMapping("/pipelines/{id}")
    public ResponseEntity<ExportPipelineResponse> getPipeline(
            @PathVariable("id") long id,
            @CurrentUser UserPrincipal user) {
        return ResponseEntity.ok(exportPipelineService.getPipeline(id, user));
    }

    @GetMapping("/pipelines/{id}/status")
    public ResponseEntity<ExportJobStatusResponse> getPipelineJobStatus(
            @PathVariable("id") long id,
            @CurrentUser UserPrincipal user) {
        return ResponseEntity.ok(exportPipelineService.getPipelineStatus(id, user));
    }

    @GetMapping("/pipelines/{id}/download")
    public ResponseEntity<DownloadUrlDto> getPipelineDownloadUrl(
            @PathVariable("id") long id,
            @CurrentUser UserPrincipal user) throws IOException {
        return ResponseEntity.ok(exportPipelineService.getDownloadUrl(id, user));
    }

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

    @PostMapping("/pipelines/{id}/retry")
    public ResponseEntity<ExportPipelineResponse> retry(@PathVariable("id") long id, @CurrentUser UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(exportPipelineService.retryPipeline(id, user));
    }

    @DeleteMapping("/pipelines/{id}")
    public ResponseEntity<Void> deletePipeline(@PathVariable("id") long id, @CurrentUser UserPrincipal user) {
        exportPipelineService.deletePipeline(id, user);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/fields")
    public ResponseEntity<List<ExportFieldSchema>> getAvailableFields() {
        return ResponseEntity.ok(exportPipelineService.getAvailableFields());
    }

}
