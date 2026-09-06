package com.bioinformatics.exportservice.dto;

import java.time.Instant;

/**
 * DTO for exporting pipeline execution status and progress.
 *
 * <p>Used by the `/api/exports/pipelines/{id}/status` endpoint for real-time polling.
 * Frontend polls this every 3 seconds to display progress to the user.
 *
 * <p>Contains:
 * <ul>
 *   <li>Pipeline ID and current status
 *   <li>Progress percentage (0–100)
 *   <li>Chunk-level progress (if running)
 *   <li>Last update timestamp
 * </ul>
 */
public record ExportJobStatusResponse(
        Long pipelineId,
        ExportStatus status,
        Integer progressPercent,
        Integer chunksProcessed,
        Integer chunksTotal,
        String currentStep,
        Instant updatedAt
) {
}

