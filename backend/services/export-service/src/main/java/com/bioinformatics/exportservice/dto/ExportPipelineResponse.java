package com.bioinformatics.exportservice.dto;

import com.bioinformatics.common.models.gene.GeneSearchRequest;

import java.time.Instant;
import java.util.List;

/**
 * DTO for export pipeline API response.
 *
 * <p>Contains the full state of an export pipeline including:
 * <ul>
 *   <li>Configuration (name, format, selected fields)
 *   <li>Status and progress (estimated/actual rows)
 *   <li>File information (path, size)
 *   <li>Lifecycle timestamps (created, completed, duration)
 * </ul>
 *
 * <p>Used by both detail and list endpoints.
 */
public record ExportPipelineResponse(
        Long id,
        String name,
        String description,
        ExportFormat format,
        GeneSearchRequest filter,
        List<String> fieldSchema,
        ExportStatus status,
        Long estimatedRows,
        Long actualRows,
        String filePath,
        Long fileSizeBytes,
        String errorMessage,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        Long durationMs
) {
}

